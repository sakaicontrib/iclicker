/**
 * Copyright (c) 2009 i>clicker (R) <http://www.iclicker.com/dnn/>
 *
 * This file is part of i>clicker Sakai integrate.
 *
 * i>clicker Sakai integrate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * i>clicker Sakai integrate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with i>clicker Sakai integrate.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sakaiproject.iclicker.logic;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.sakaiproject.genericdao.api.search.Order;
import org.sakaiproject.genericdao.api.search.Restriction;
import org.sakaiproject.genericdao.api.search.Search;
import org.sakaiproject.iclicker.api.dao.IClickerDao;
import org.sakaiproject.iclicker.api.logic.BigRunner;
import org.sakaiproject.iclicker.exception.ClickerIdInvalidException;
import org.sakaiproject.iclicker.exception.ClickerLockException;
import org.sakaiproject.iclicker.exception.ClickerRegisteredException;
import org.sakaiproject.iclicker.exception.ClickerIdInvalidException.Failure;
import org.sakaiproject.iclicker.model.Course;
import org.sakaiproject.iclicker.model.Gradebook;
import org.sakaiproject.iclicker.model.GradebookItem;
import org.sakaiproject.iclicker.model.GradebookItemScore;
import org.sakaiproject.iclicker.model.Student;
import org.sakaiproject.iclicker.model.User;
import org.sakaiproject.iclicker.model.dao.ClickerRegistration;
import org.sakaiproject.iclicker.model.dao.ClickerUserKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

/**
 * This is the implementation of the business logic interface, this handles all the business logic and processing for the application
 */
public class IClickerLogic {

    private static final Logger log = LoggerFactory.getLogger(IClickerLogic.class);

    public static final String VERSION = "11-SNAPSHOT"; // should match the POM version
    public static final String VERSION_DATE = "20161222"; // the date in YYYYMMDD

    // CONFIG
    public static final String DEFAULT_SERVER_URL = "http://localhost/sakai";
    public String serverId = "UNKNOWN_SERVER_ID";
    public String serverURL = DEFAULT_SERVER_URL;
    public String domainURL = DEFAULT_SERVER_URL;
    public String workspacePageTitle = "i>clicker";
    public boolean disableAlternateRemoteID = false;
    public boolean forceRestDebugging = false;

    private String notifyEmailsString = null;
    private String[] notifyEmails = null;
    @Getter private boolean singleSignOnHandling = false;
    private String singleSignOnSharedkey = null;
    private int maxCoursesForInstructor = 100;
    private static final int CLICKERID_LENGTH = 8;
    @Getter private List<String> failures = new Vector<>();

    /**
     * Special tracker to see if the system is already running a thread, this is meant to ensure that more than one large scale operation is not running at once
     */
    private WeakReference<BigRunner> runnerHolder;
    private static final String ICLICKER_TOOL_ID = "sakai.iclicker";

    @Setter private IClickerDao dao;
    @Setter @Getter private ExternalLogic externalLogic;
    @Setter private ReloadableResourceBundleMessageSource messageSource;
    protected static IClickerLogic instance;

    /**
     * Place any code that should run when this class is initialized by spring here
     */
    public void init() {
        log.info("INIT");
        // store this so we can get the service later
        IClickerLogic.setInstance(this);
        serverURL = externalLogic.getConfigurationSetting(ExternalLogic.SETTING_SERVER_URL, DEFAULT_SERVER_URL);
        domainURL = serverURL;
        workspacePageTitle = externalLogic.getConfigurationSetting("iclicker.workspace.title", workspacePageTitle);
        disableAlternateRemoteID = externalLogic.getConfigurationSetting("iclicker.turn.off.alternate.remote.id", disableAlternateRemoteID);
        serverId = externalLogic.getConfigurationSetting(AbstractExternalLogic.SETTING_SERVER_ID, serverId);
        forceRestDebugging = externalLogic.getConfigurationSetting("iclicker.rest.debug", false);
        maxCoursesForInstructor = externalLogic.getConfigurationSetting("iclicker.max.courses", maxCoursesForInstructor);

        notifyEmailsString = externalLogic.getConfigurationSetting("iclicker.notify.emails", notifyEmailsString);

        if (notifyEmailsString == null) {
            // get from the server
            String email = externalLogic.getNotificationEmail();

            if (email != null) {
                notifyEmails = new String[] {email};
            }
        } else {
            notifyEmails = notifyEmailsString.split(",");

            if (notifyEmails.length == 0) {
                notifyEmails = null;
                log.warn("Invalid list of email addresses in iclicker.notify.emails config setting: {}", notifyEmailsString);
            } else {
                for (int i = 0; i < notifyEmails.length; i++) {
                    notifyEmails[i] = notifyEmails[i].trim();
                }
            }
        }

        // Special SSO handling
        setSharedKey(externalLogic.getConfigurationSetting("iclicker.sso.shared.key", (String) null));
    }

    /**
     * @return true if SSO handling is enabled, false otherwise
     */
    public boolean isSingleSignOnEnabled() {
        return singleSignOnHandling;
    }

    /**
     * Sends a notification to the list of admins, this is primarily for notifications of failures related to webservices failures
     * 
     * @param message the notification message to send
     * @param failure [OPTIONAL] the exception if there was one
     */
    public void sendNotification(String message, Exception failure) {
        String body = "i>clicker Sakai integrate plugin notification (" + new Date() + ")\n" + message + "\n";

        if (failure != null) {
            // get the stacktrace out
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            failure.printStackTrace(pw);
            String stacktrace = "Full stacktrace:\n" + failure.getClass().getSimpleName() + ":" + failure.getMessage() + ":\n" + sw.toString();
            body = body + "\nFailure:\n" + failure.toString() + "\n\n" + stacktrace;

            // add to failures record and trim it
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            failures.add(df.format(new Date()) + " :: " + StringUtils.abbreviate(message, 300));

            while (failures.size() > 3) {
                failures.remove(0);
            }
        }

        if (notifyEmails != null && notifyEmails.length > 0) {
            externalLogic.sendEmails(null, notifyEmails, "i>clicker Sakai integrate plugin notification", body);
        } else {
            log.warn("No emails set for sending notifications: logging notification: {}", body);
        }
    }

    /**
     * resolve the i18n message
     * 
     * @param key the message key to lookup
     * @param locale the Locale in which to do the lookup
     * @param args Array of arguments that will be filled in for params within the message (params look like "{0}", "{1,date}", "{2,time}" within a message), or null if none
     * @return the i18n string OR null if the key cannot be resolved
     */
    public String getMessage(String key, Locale locale, Object... args) {
        String msg;

        try {
            msg = messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            msg = null;
        }

        return msg;
    }

    // *******************************************************************************
    // SSO handling

    /**
     * Attempt to authenticate a user given a login name and password (or user passkey)
     * 
     * @param loginname the login name for the user
     * @param password the password for the user (might be the SSO user passkey)
     * @param ssoKey [OPTIONAL] the SSO encoded key if one exists in the request
     * @return the session ID if authenticated OR null if the auth parameters are invalid
     */
    public String authenticate(String loginname, String password, String ssoKey) {
        String sessionId;

        if (singleSignOnHandling) {
            verifyKey(ssoKey); // verify the key is valid

            String userId = externalLogic.getUserIdFromLoginName(loginname);

            if (userId == null) {
                // invalid user!
                sessionId = null;
            } else {
                // check the user password against the SSO passkey
                if (checkUserKey(userId, password)) {
                    // valid user and key so generate the session
                    sessionId = externalLogic.startUserSessionById(userId);
                } else {
                    // invalid user key
                    sessionId = null;
                    log.warn("Invalid user key passed in ({}) for user ({})", password, userId);
                }
            }
        } else {
            sessionId = externalLogic.authenticateUser(loginname, password, true);
        }

        return sessionId;
    }

    /**
     * Make or find a user key for the given user, if they don't have one, this will create one, if they do it will retrieve it. It can also be used to generate a new user key
     * 
     * @param userId [OPTIONAL] the internal user id (if null, use the current user id)
     * @param makeNew if true, make a new key even if the user already has one, if false, only make a key if they do not have one
     * @return the user key for the given user
     * @throws IllegalStateException if user is not set or is not an instructor
     */
    public String makeUserKey(String userId, boolean makeNew) {
        if (userId == null) {
            userId = externalLogic.getCurrentUserId();
        }
        if (userId == null) {
            throw new IllegalStateException("no current user, cannot generate a user key");
        }

        if (!externalLogic.isUserInstructor(userId) && !externalLogic.isUserAdmin(userId)) {
            // if user is not an instructor or an admin then we will not make a key for them, this is to block students from getting a pass key
            throw new IllegalStateException("current user (" + userId + ") is not an instructor, cannot generate user key for them");
        }

        // find the key for this user if one exists
        ClickerUserKey cuk = dao.findOneBySearch(ClickerUserKey.class, new Search(new Restriction("userId", userId)));

        if (makeNew && cuk != null) {
            // remove the existing key so we can make a new one
            dao.delete(cuk);
            cuk = null;
        }

        if (cuk == null) {
            // make a new key and store it
            String newKeyValue = RandomStringUtils.randomAlphanumeric(12);
            cuk = new ClickerUserKey(newKeyValue, userId);

            try {
                dao.save(cuk);
            } catch (DataIntegrityViolationException e) {
                // this should not have happened but it means the key already exists somehow, probably a sync issue of some kind
                log.warn("Failed when attempting to create a new clicker user key for: {}", userId);
            }
        }

        return cuk.getUserKey();
    }

    /**
     * Checks if the passed in user key is valid compared to the internally stored user key
     * 
     * @param userId [OPTIONAL] the internal user id (if null, use the current user id)
     * @param userKey the passed in SSO key to check for this user
     * @return true if the key is valid OR false if the user has no key or the key is otherwise invalid
     */
    public boolean checkUserKey(String userId, String userKey) {
        if (StringUtils.isEmpty(userKey)) {
            throw new IllegalArgumentException("userKey cannot be empty");
        }
        if (userId == null) {
            userId = externalLogic.getCurrentUserId();
        }
        if (userId == null) {
            throw new IllegalStateException("no current user, cannot check user key");
        }

        boolean valid = false;
        ClickerUserKey cuk = dao.findOneBySearch(ClickerUserKey.class, new Search(new Restriction("userId", userId)));

        if (cuk != null) {
            if (StringUtils.equals(userKey, cuk.getUserKey())) {
                valid = true;
            }
        }

        if (forceRestDebugging) {
            log.info("REST debug checkUserKey (u={}, v={}): {}", userId, valid, cuk);
        }

        return valid;
    }

    /**
     * @param sharedKey set and verify the shared key (if invalid, log a warning)
     */
    public void setSharedKey(String sharedKey) {
        if (sharedKey != null) {
            if (sharedKey.length() < 10) {
                log.warn("i>clicker shared key ({}) is too short, must be at least 10 chars long. SSO shared key will be ignored until a longer key is entered.", sharedKey);
            } else {
                singleSignOnHandling = true;
                singleSignOnSharedkey = sharedKey;
                log.info("i>clicker plugin SSO handling enabled by shared key, note that this will disable normal username/password handling");
            }
        }
    }

    /**
     * @return the SSO shared key value (or empty string otherwise) NOTE: this only works if SSO is enabled AND the user is an admin
     */
    public String getSharedKey() {
        String key = "";

        if (singleSignOnHandling && externalLogic.isUserAdmin(externalLogic.getCurrentUserId())) {
            key = singleSignOnSharedkey;
        }

        return key;
    }

    /**
     * Verify the passed in encrypted SSO shared key is valid, this will return false if the key is not configured Key must have been encoded like so (where timestamp is the unix time in seconds):
     * sentKey = hex(sha1(sharedKey + ":" + timestamp)) + "|" + timestamp
     * 
     * @param key the passed in key (should already be sha-1 and hex encoded with the timestamp appended)
     * @return true if the key is valid, false if SSO shared keys are disabled
     * @throws IllegalArgumentException if the key format is invalid
     * @throws SecurityException if the key timestamp has expired or the key does not match
     */
    public boolean verifyKey(String key) {
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("key must be set in order to verify the key");
        }

        boolean verified = false;

        if (singleSignOnHandling) {
            // encoding process requires the key and timestamp so split them from the passed in key
            int splitIndex = key.lastIndexOf('|');

            if ((splitIndex == -1) || (key.length() < splitIndex + 1)) {
                throw new IllegalArgumentException("i>clicker shared key (" + key + ") format is invalid (no |), must be {encoded key}|{timestamp}");
            }

            String actualKey = key.substring(0, splitIndex);

            if (StringUtils.isEmpty(actualKey)) {
                throw new IllegalArgumentException("i>clicker shared key (" + key + ") format is invalid (missing encoded key), must be {encoded key}|{timestamp}");
            }

            String timestampStr = key.substring(splitIndex + 1);

            if (StringUtils.isEmpty(timestampStr)) {
                throw new IllegalArgumentException("i>clicker shared key (" + key + ") format is invalid (missing timestamp), must be {encoded key}|{timestamp}");
            }

            long timestamp;

            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("i>clicker shared key (" + key + ") format is invalid (non numeric timestamp), must be {encoded key}|{timestamp}");
            }

            // check this key is still good (must be within 5 mins of now)
            long unixTime = System.currentTimeMillis() / 1000L;
            long timeDiff = Math.abs(timestamp - unixTime);

            if (timeDiff > 300L) {
                throw new SecurityException("i>clicker shared key (" + key + ") timestamp is out of date, this timestamp (" + timestamp + ") is more than 5 minutes different from the current time (" + unixTime + ")");
            }

            // finally we verify the key with the one in the config
            byte[] sha1Bytes = DigestUtils.sha(singleSignOnSharedkey + ":" + timestamp);
            String sha1Hex = Hex.encodeHexString(sha1Bytes);

            if (!StringUtils.equals(actualKey, sha1Hex)) {
                throw new SecurityException("i>clicker encoded shared key (" + key + ") does not match with the key (" + sha1Hex + ") in Sakai (using timestamp: " + timestamp + ")");
            }

            verified = true;
        }

        return verified;
    }

    // *******************************************************************************
    // Admin i>clicker tool in workspace handling

    /**
     * Executes the add or remove tool workspace operation
     * 
     * @param type the type of runner to make, from BigRunner.RUNNER_TYPE_*
     * @return the runner object indicating progress
     * @throws IllegalArgumentException if the runner type is unknown
     * @throws IllegalStateException if there is a runner in progress of a different type
     * @throws ClickerLockException if a lock cannot be obtained
     */
    public BigRunner startRunnerOperation(String type) {
        BigRunner runner = getRunnerStatus();

        if (runner != null) {
            // allow the runner to be cleared if done
            if (runner.isComplete()) {
                runner = null;
            } else {
                if (!StringUtils.equals(type, runner.getType())) {
                    throw new IllegalStateException("Already running a big runner of a different type: " + runner.getType());
                }
            }
        }

        if (runner == null) {
            // try to obtain a lock
            Boolean gotLock = dao.obtainLock(BigRunner.RUNNER_LOCK, serverId, 600000); // expire 10 mins

            if (gotLock != null && gotLock) {
                if (BigRunner.RUNNER_TYPE_ADD.equalsIgnoreCase(type)) {
                    runner = getExternalLogic().makeAddToolToWorkspacesRunner(ICLICKER_TOOL_ID, workspacePageTitle, null);
                } else if (BigRunner.RUNNER_TYPE_REMOVE.equalsIgnoreCase(type)) {
                    runner = getExternalLogic().makeRemoveToolFromWorkspacesRunner(ICLICKER_TOOL_ID);
                } else {
                    throw new IllegalArgumentException("Unknown type of runner operation: " + type);
                }

                final BigRunner bigRunner = runner;
                Runnable runnable = new Runnable() {
                    public void run() {
                        try {
                            bigRunner.run();
                        } catch (Exception e) {
                            String msg = "long running process (" + bigRunner + ") failure: " + e;
                            sendNotification(msg, e);
                            log.warn(msg, e);
                            // sleep for 5 secs to hold the error state so it can be checked
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e1) {
                                // nothing to do here
                            }
                        } finally {
                            clearRunner(); // when done
                            dao.releaseLock(BigRunner.RUNNER_LOCK, serverId);
                        }
                    }
                };

                new Thread(runnable).start(); // start up a thread to run this
                log.info("Starting new long running process ({})", runner);
                this.runnerHolder = new WeakReference<BigRunner>(runner);
            } else {
                // failed to obtain the lock
                String msg = "Could not obtain a lock (" + BigRunner.RUNNER_LOCK + ") on server (" + serverId + "): " + gotLock;
                log.info(msg);
                throw new ClickerLockException(msg, BigRunner.RUNNER_LOCK, serverId);
            }
        }

        return runner;
    }

    /**
     * Get the currently running process if there is one
     * 
     * @return the runner OR null if there is no running process
     */
    public BigRunner getRunnerStatus() {
        return this.runnerHolder != null ? this.runnerHolder.get() : null;
    }

    /**
     * clears the holder
     */
    public void clearRunner() {
        if (runnerHolder != null) {
            runnerHolder.clear();
            runnerHolder = null;
        }
    }

    // *******************************************************************************
    // Clicker registration handling

    /**
     * This returns an item based on an id if the user is allowed to access it
     * 
     * @param id the id of the item to fetch
     * @return a ClickerRegistration or null if none found
     * @throws SecurityException if the current user cannot access this item
     */
    public ClickerRegistration getItemById(Long id) {
        log.debug("Getting item by id: {}", id);
        ClickerRegistration item = dao.findById(ClickerRegistration.class, id);

        if (item != null) {
            if (!canReadItem(item, externalLogic.getCurrentUserId())) {
                throw new SecurityException("User (" + externalLogic.getCurrentUserId() + ") not allowed to access registration (" + item + ")");
            }
        }

        return item;
    }

    /**
     * This returns an item based on a clickerId for the current user if allowed to access it, this will return a null if the clickerId happens to be invalid or cannot be found
     * 
     * @param clickerId the clicker remote ID
     * @return a ClickerRegistration OR null if none found
     * @throws SecurityException if the current user cannot access this item
     */
    public ClickerRegistration getItemByClickerId(String clickerId) {
        return getItemByClickerId(clickerId, null);
    }

    /**
     * This returns an item based on a clickerId and ownerId if the user is allowed to access it, this will return a null if the clickerId is invalid or cannot be found
     * 
     * @param clickerId the clicker remote ID
     * @param ownerId the clicker owner ID (user id)
     * @return a ClickerRegistration OR null if none found
     * @throws SecurityException if the current user cannot access this item
     */
    public ClickerRegistration getItemByClickerId(String clickerId, String ownerId) {
        log.debug("Getting item by clickerId: {}", clickerId);
        String userId = externalLogic.getCurrentUserId();

        if (userId == null) {
            throw new SecurityException("user must be logged in");
        }

        if (ownerId != null) {
            userId = ownerId;
        }

        try {
            clickerId = validateClickerId(clickerId);
        } catch (ClickerIdInvalidException e) {
            return null;
        }

        ClickerRegistration item = dao.findOneBySearch(ClickerRegistration.class, new Search(new Restriction[] {new Restriction("clickerId", clickerId), new Restriction("ownerId", userId)}));
        if (item != null) {
            if (!canReadItem(item, externalLogic.getCurrentUserId())) {
                throw new SecurityException("User (" + externalLogic.getCurrentUserId() + ") not allowed to access registration (" + item + ")");
            }
            if (!isValidClickerRegistration(item)) {
                // invalid registration, don't return it
                return null;
            }
        }

        return item;
    }

    /**
     * @param item registration
     * @param userId sakai user id
     * @return true if the item can be read, false otherwise
     */
    public boolean canReadItem(ClickerRegistration item, String userId) {
        log.debug("checking if can read for: {} and item={}", userId, item);
        String locationId = "";

        if (StringUtils.equals(item.getOwnerId(), userId)) {
            // owner can always read an item
            return true;
        } else if (externalLogic.isUserAdmin(userId)) {
            // the system super user can read any item
            return true;
        } else if (StringUtils.equals(locationId, item.getLocationId())) {
            // users with permission in the specified site can modify items from that site
            return true;
        } else if (externalLogic.isInstructorOfUser(item.getOwnerId()) != null) {
            // we are allowing instructors to read items for students in their course
            return true;
        }

        return false;
    }

    /**
     * Check if a specified user can write this item in a specified site
     * 
     * @param item to be modified or removed
     * @param userId the internal user id (not username)
     * @return true if item can be modified, false otherwise
     */
    public boolean canWriteItem(ClickerRegistration item, String userId) {
        log.debug("checking if can write for: {} and item={}", userId, item);

        if (StringUtils.equals(item.getOwnerId(), userId)) {
            // owner can always modify an item
            return true;
        } else if (externalLogic.isUserAdmin(userId)) {
            // the system super user can modify any item
            return true;
        } else if (externalLogic.isInstructorOfUser(item.getOwnerId()) != null) {
            // we are allowing instructors to write items for students in their course
            return true;
        }

        return false;
    }

    /**
     * This returns a List of items that are visible for a specified user
     * 
     * @param userId the internal user id (not username)
     * @param locationId [OPTIONAL] a unique id which represents the current location of the user (entity reference)
     * @return a List of ClickerRegistration objects
     */
    public List<ClickerRegistration> getAllVisibleItems(String userId, String locationId) {
        log.debug("Fetching visible items for {} in site: {}", userId, locationId);
        List<ClickerRegistration> l;

        if (locationId == null) {
            // get the items for this user only
            l = dao.findBySearch(ClickerRegistration.class, new Search(new Restriction[] {new Restriction("ownerId",
                            userId), new Restriction("activated", true)}));
        } else {
            // inst gets registrations for themselves only
            // TAs still able to get their remote registrations this way
            // search by location would not be helpful here
            if (externalLogic.isUserAdmin(userId)) {
                // admin gets all items when requesting by location?
                l = dao.findAll(ClickerRegistration.class);
            } else {
                // student gets registrations for themselves only always
                l = dao.findBySearch(ClickerRegistration.class, new Search(new Restriction[] {new Restriction("ownerId",
                                userId), new Restriction("activated", true)}));
            }
        }

        return l;
    }

    /**
     * ADMIN ONLY Only the admin can use this method to retrieve all clicker IDs
     * 
     * @param first the first item (for paging and limiting)
     * @param max the max number of items to return
     * @param order [OPTIONAL] sort order for the items
     * @param searchStr [OPTIONAL] search by partial clickerId
     * @param includeUserDisplayNames if true the user display names are added to the results
     * @return a list of clicker registrations
     */
    public List<ClickerRegistration> getAllItems(int first, int max, String order, String searchStr, boolean includeUserDisplayNames) {
        // admin only
        if (!externalLogic.isUserAdmin(externalLogic.getCurrentUserId())) {
            throw new SecurityException("Only admins can get the listing of all clicker registrations");
        }

        if (StringUtils.isBlank(order)) {
            order = "clickerId";
        }

        Search s = new Search();
        s.setStart(first);
        s.setLimit(max);
        s.addOrder(new Order(order));

        if (!StringUtils.isBlank(searchStr)) {
            // maybe allow search on more than clickerId later
            s.addRestriction(new Restriction("clickerId", searchStr, Restriction.LIKE));
        }

        List<ClickerRegistration> l = dao.findBySearch(ClickerRegistration.class, s);

        // optionally include the user names
        if (includeUserDisplayNames) {
            for (ClickerRegistration reg : l) {
                reg.userDisplayName = getUserDisplayName(reg.getOwnerId());
            }
        }

        return l;
    }

    public int countAllItems() {
        return dao.countAll(ClickerRegistration.class);
    }

    public String getUserDisplayName(String userId) {
        if (StringUtils.isBlank(userId)) {
            userId = externalLogic.getCurrentUserId();
        }

        String name = externalLogic.getUserDisplayName(userId);

        if (StringUtils.isBlank(name)) {
            name = userId;
        }

        return name;
    }

    /**
     * Remove an item NOTE: only admins can fully remove a registration
     * 
     * @param item the ClickerRegistration to remove
     * @throws SecurityException if the user not allowed to remove the registration
     */
    public void removeItem(ClickerRegistration item) {
        log.debug("In removeItem with item: {}", item);

        // check if current user can remove this item
        if (externalLogic.isUserAdmin(externalLogic.getCurrentUserId())) {
            dao.delete(item);
            log.info("Removing clicker registration: {}", item);
        } else {
            throw new SecurityException("Current user cannot remove registration " + item + " because they do not have permission, only admins can remove");
        }
    }

    /**
     * Save (Create or Update) an item (uses the current site)
     * 
     * @param item the ClickerRegistration to create or update
     * @throws IllegalArgumentException if the item is null OR the owner id is not a valid user
     * @throws SecurityException if the user cannot save the registration for lack of perms
     * @throws ClickerIdInvalidException if the clicker ID is invalid for some reason, the exception will indicate the type of validation failure
     */
    public void saveItem(ClickerRegistration item) {
        log.debug("In saveItem with item: {}", item);

        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        if (!isValidClickerRegistration(item)) {
            throw new ClickerIdInvalidException("Invalid clicker registration.", Failure.LENGTH, item.getClickerId());
        }

        String clickerId = StringUtils.trimToNull(item.getClickerId());

        if (item.isActivated() || clickerId == null) {
            // only validate when activating or clearly invalid
            clickerId = validateClickerId(item.getClickerId());
        }

        item.setClickerId(clickerId);

        // set the owner to current if not set
        if (item.getOwnerId() == null) {
            item.setOwnerId(externalLogic.getCurrentUserId());
        } else {
            // check for valid user id
            User u = externalLogic.getUser(item.getOwnerId());

            if (u == null) {
                throw new IllegalArgumentException("user id (" + item.getOwnerId() + ") is invalid (cannot match to user)");
            }
        }

        Date now = new Date();

        if (item.getDateCreated() == null) {
            item.setDateCreated(now);
        }

        item.setDateModified(now);

        // save item if new OR check if the current user can update the existing item
        if ((item.getId() == null) || canWriteItem(item, externalLogic.getCurrentUserId())) {
            dao.save(item);
            log.info("Saving clicker registration: {}", item);
        } else {
            throw new SecurityException("Current user cannot update item " + item.getId() + " because they do not have permission");
        }
    }

    /**
     * Creates a new clicker remote registration in the system for the current user, also push to national
     * 
     * @param clickerId the clicker remote ID
     * @return the registration
     * @throws ClickerIdInvalidException if the clicker ID is invalid for some reason, the exception will indicate the type of validation failure
     * @throws ClickerRegisteredException if the clickerId is already registered
     * @throws SecurityException if the user cannot save the registration for lacks of perms
     */
    public ClickerRegistration createRegistration(String clickerId) {
        return createRegistration(clickerId, null);
    }

    /**
     * Creates a new clicker remote registration in the system, will push the registration to national as well
     * 
     * @param clickerId the clicker remote ID
     * @param ownerId the owner of this registration
     * @return the registration
     * @throws ClickerIdInvalidException if the clicker ID is invalid for some reason, the exception will indicate the type of validation failure
     * @throws ClickerRegisteredException if the clickerId is already registered
     * @throws IllegalArgumentException if the owner id is not a valid user
     * @throws SecurityException if the user cannot save the registration for lacks of perms
     */
    public ClickerRegistration createRegistration(String clickerId, String ownerId) {
        clickerId = validateClickerId(clickerId);
        String currentUserId = externalLogic.getCurrentUserId();

        if (currentUserId == null) {
            throw new SecurityException("user must be logged in");
        }

        String userId = ownerId;

        if (StringUtils.isBlank(ownerId)) {
            userId = currentUserId;
        }

        ClickerRegistration registration = getItemByClickerId(clickerId, userId);

        // NOTE: we probably want to check the national system here to see if this is already registered
        if (registration != null) {
            if (StringUtils.equals(registration.getOwnerId(), currentUserId)) {
                // reactivate the clicker if needed
                if (!registration.isActivated()) {
                    registration.setActivated(true);
                    saveItem(registration);
                }
            } else {
                throw new ClickerRegisteredException(userId, registration.getClickerId(), registration.getOwnerId());
            }
        } else {
            registration = new ClickerRegistration(clickerId, userId);
            saveItem(registration);
        }

        return registration;
    }

    /**
     * Updates the activation of a registration for the current user
     * 
     * @param registrationId the unique id of the registration
     * @param activated the new activation level
     * @return the registration if it was updated, null if not updated
     * @throws IllegalArgumentException if the registrationId is invalid
     * @throws SecurityException is the current user cannot update the registration
     */
    public ClickerRegistration setRegistrationActive(Long registrationId, boolean activated) {
        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId cannot be null");
        }

        ClickerRegistration registration = getItemById(registrationId);

        if (registration == null) {
            throw new IllegalArgumentException("Could not find registration with id: " + registrationId);
        }

        String userId = externalLogic.getCurrentUserId();

        if (!canWriteItem(registration, userId)) {
            throw new SecurityException("User (" + userId + ") cannot update registration (" + registration + ")");
        }

        boolean current = registration.isActivated();

        if (current != activated) {
            registration.setActivated(activated);
            saveItem(registration);
            return registration;
        }

        return null;
    }

    public List<Student> getStudentsForCourseWithClickerReg(String courseId) {
        List<Student> students = externalLogic.getStudentsForCourse(courseId);

        if (students != null && !students.isEmpty()) {
            // populate clickerRegistration data
            // get the set of all registrations
            Search search = new Search();
            search.addRestriction(new Restriction("activated", true)); // only active ones

            // noinspection StatementWithEmptyBody
            if (students.size() <= 500) {
                String[] owners = new String[students.size()];

                for (int i = 0; i < students.size(); i++) {
                    owners[i] = students.get(i).getUserId();
                }

                search.addRestriction(new Restriction("ownerId", owners));
            }

            search.addOrder(new Order("ownerId"));
            List<ClickerRegistration> l = dao.findBySearch(ClickerRegistration.class, search);
            l = removeInvalidClickerRegistrations(l);
            // create map of registrations to owners
            HashMap<String, Set<ClickerRegistration>> ownerToReg = new HashMap<>();

            for (ClickerRegistration cr : l) {
                if (!ownerToReg.containsKey(cr.getOwnerId())) {
                    ownerToReg.put(cr.getOwnerId(), new HashSet<ClickerRegistration>());
                }

                ownerToReg.get(cr.getOwnerId()).add(cr);
            }

            // now merge the set of registrations with the set of students
            for (Student student : students) {
                Set<ClickerRegistration> crs = ownerToReg.get(student.getUserId());

                if (crs == null || crs.isEmpty()) {
                    student.setClickerRegistered(Boolean.FALSE);
                } else {
                    student.setClickerRegistered(Boolean.TRUE);
                    student.setClickerRegistrations(crs);
                }
            }
        }

        return students;
    }

    /**
     * Get the clicker registrations for a given user
     * 
     * @param userId the id of the user
     * @param activeOnly if true only get the active registrations, else get all
     * @return the list of registrations for this user
     */
    public List<ClickerRegistration> getClickerRegistrationsForUser(String userId, boolean activeOnly) {
        Search search = new Search();

        if (activeOnly) {
            search.addRestriction(new Restriction("activated", true)); // only active ones
        }

        search.addRestriction(new Restriction("ownerId", userId));
        search.addOrder(new Order("dateCreated", false));

        return dao.findBySearch(ClickerRegistration.class, search);
    }

    /**
     * Get all the courses for the given user, note that this needs to be limited from outside this method for security, if the return is limited to a single course then the students are included
     * 
     * @param courseId [OPTIONAL] limit the return to just this one site
     * @return the courses (up to 100 of them) which the user has instructor access in
     */
    public List<Course> getCoursesForInstructorWithStudents(String courseId) {
        List<Course> courses = externalLogic.getCoursesForInstructor(courseId, maxCoursesForInstructor);

        if (courseId != null && courses.size() == 1) {
            // add in the students
            Course c = courses.get(0);
            c.setStudents(getStudentsForCourseWithClickerReg(c.getId()));
        }

        return courses;
    }

    /**
     * @param courseId a unique id which represents the current location of the user (entity reference)
     * @return the title for the context or "--------" (8 hyphens) if none found
     */
    public String getCourseTitle(String courseId) {
        return externalLogic.getLocationTitle(courseId);
    }

    /**
     * Gets the gradebook data for a given site, this uses the gradebook security so it is safe for anyone to call
     * 
     * @param courseId a sakai siteId (cannot be group Id)
     * @param gbItemName [OPTIONAL] an item name to fetch from this gradebook (limit to this item only), if null then all items are returned
     */
    public Gradebook getCourseGradebook(String courseId, String gbItemName) {
        Gradebook gb = externalLogic.getCourseGradebook(courseId, gbItemName);
        gb.setStudents(getStudentsForCourseWithClickerReg(courseId));

        return gb;
    }

    /**
     * Save a gradebook item and optionally the scores within <br/>
     * Scores must have at least the studentId or username AND the grade set
     * 
     * @param gbItem the gradebook item to save, must have at least the gradebookId and name set
     * @return the updated gradebook item and scores, contains any errors that occurred
     */
    public GradebookItem saveGradebookItem(GradebookItem gbItem) {
        return externalLogic.saveGradebookItem(gbItem);
    }

    /**
     * Save a gradebook (saves all items in the gradebook)
     * 
     * @param gb the gradebook to save
     * @return the updated gradebook items and scores, contains any errors that occurred
     */
    public List<GradebookItem> saveGradebook(Gradebook gb) {
        ArrayList<GradebookItem> items = new ArrayList<>();

        if (gb != null) {
            for (GradebookItem gradebookItem : gb.getItems()) {
                gradebookItem.setGradebookId(gb.getId());
                items.add(saveGradebookItem(gradebookItem));
            }
        }

        return items;
    }

    // **********************************************************************************
    // DATA encoding methods - put the data into the format desired by iclicker

    public String encodeClickerRegistration(ClickerRegistration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration must be set");
        }
        /*
         * SAMPLE <Register> <S FirstName="Tim" LastName="Stelzer" StudentID="tstelzer" URL="http://www.iclicker.com/" clickerID="11111111" Enabled="True"></S> </Register>
         */
        User user = externalLogic.getUser(registration.getOwnerId());

        if (user == null) {
            throw new IllegalStateException("Could not get info about the user (" + registration.getOwnerId() + ") related to this clicker registration");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<Register>\n");
        sb.append("  <S DisplayName=\"");
        sb.append(escapeForXML(user.getName()));
        sb.append("\" FirstName=\"");
        sb.append(escapeForXML(user.getFname()));
        sb.append("\" LastName=\"");
        sb.append(escapeForXML(user.getLname()));
        sb.append("\" StudentID=\"");
        sb.append(escapeForXML(user.getUsername()).toUpperCase());
        sb.append("\" Email=\"");
        sb.append(escapeForXML(user.getEmail()));
        sb.append("\" URL=\"");
        sb.append(escapeForXML(domainURL));
        sb.append("\" ClickerID=\"");
        sb.append(escapeForXML(registration.getClickerId()).toUpperCase());
        sb.append("\" Enabled=\"");
        sb.append(registration.isActivated() ? "True" : "False");
        sb.append("\"></S>\n");
        // close out
        sb.append("</Register>\n");

        return sb.toString();
    }

    /**
     * Encode response from registration of clicker data This option should be available where the instructor already has the clicker registration file (Remoteid.csv) and wants to upload the
     * registration(s) to the CMS Server.
     * 
     * @param registrations the registrations resulting from the action
     * @param status true if new registration, false otherwise
     * @param message the human readable message
     * @return the encoded XML
     * @throws IllegalStateException if the user cannot be found
     * @throws IllegalArgumentException if the data is invalid
     */
    public String encodeClickerRegistrationResult(List<ClickerRegistration> registrations, boolean status, String message) {
        if (registrations == null || registrations.isEmpty()) {
            throw new IllegalArgumentException("registrations must be set");
        }
        if (message == null) {
            throw new IllegalArgumentException("message must be set");
        }
        /*
         * SAMPLE 1) When clicker is already registered to some one else - the same message should be returned that is displayed in the plug-in in xml format <RetStatus Status="False" Message=""/> 2)
         * When clicker is already registered to the same user - the same message should be returned that is displayed in the plug-in in xml format. <RetStatus Status="False" Message=""/> 3) When
         * studentid is not found in the CMS <RetStatus Status="False" Message="Student not found in the CMS"/> 4) Successful registration - <RetStatus Status="True" Message="..."/>
         */

        return "<RetStatus Status=\"" + (status ? "True" : "False") + "\" Message=\"" + escapeForXML(message) + "\" />";
    }

    /**
     * The xml format to upload students registration to the CMS Server remains the same as the national registration web services and this upload should be treated as if the user is registering the
     * remotes manually inside the plug-in i.e all applicable messages should be returned <br/>
     * NOTE: we are ignoring the email and name inputs because we will get them from the user lookup of the id
     * 
     * @param xml XML
     * @return the clicker registration object from the xml
     * @throws IllegalArgumentException if the xml is invalid or blank
     * @throws RuntimeException if there is an internal failure in the XML parser
     */
    public ClickerRegistration decodeClickerRegistration(String xml) {
        /*
         * <Register> <S DisplayName="DisplayName-azeckoski-123456" FirstName="First" LastName="Lastazeckoski-123456" StudentID="eid-azeckoski-123456" Email="azeckoski-123456@email.com"
         * URL="http://sakaiproject.org"; ClickerID="11111111"></S> </Register>
         */

        if (StringUtils.isBlank(xml)) {
            throw new IllegalArgumentException("xml must be set");
        }

        // read the xml (try to anyway)
        DocumentBuilder db;

        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException("XML parser failure: " + e, e);
        }

        Document doc;

        try {
            doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (SAXException e) {
            throw new RuntimeException("XML read failure: " + e, e);
        } catch (IOException e) {
            throw new RuntimeException("XML IO failure: " + e, e);
        }

        ClickerRegistration cr;

        try {
            doc.getDocumentElement().normalize();
            NodeList users = doc.getElementsByTagName("S");

            if (users.getLength() == 0) {
                throw new IllegalArgumentException("Invalid XML, no S element");
            }

            Node userNode = users.item(0);

            if (userNode.getNodeType() == Node.ELEMENT_NODE) {
                Element user = (Element) userNode;
                String clickerId = user.getAttribute("ClickerID");

                if (StringUtils.isBlank(clickerId)) {
                    throw new IllegalArgumentException("Invalid XML for registration, no id in the ClickerID element (Cannot process)");
                }

                String userId = user.getAttribute("StudentID"); // this is the userId

                if (StringUtils.isBlank(userId)) {
                    throw new IllegalArgumentException("Invalid XML for registration, no id in the StudentID element (Cannot process)");
                }

                cr = new ClickerRegistration(clickerId, userId);
                cr.userDisplayName = user.getAttribute("DisplayName");
            } else {
                throw new IllegalArgumentException("Invalid user node in XML: " + userNode);
            }
        } catch (DOMException e) {
            throw new RuntimeException("XML DOM parsing failure: " + e, e);
        }

        return cr;
    }

    public String encodeCourses(String instructorId, List<Course> courses) {
        if (courses == null) {
            throw new IllegalArgumentException("courses must be set");
        }
        /*
         * SAMPLE <coursemembership username="test_instructor01"> <course id="BFW61" name="BFW - iClicker Test" created="111111111" published="true" usertype="I" /> </coursemembership>
         */

        User user = externalLogic.getUser(instructorId);

        if (user == null) {
            throw new IllegalStateException("Could not get info about the user (" + instructorId + ") related to the course listing");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<coursemembership username=\"");
        sb.append(escapeForXML(user.getUsername()));
        sb.append("\">\n");

        // loop through courses
        for (Course course : courses) {
            sb.append("  <course id=\"");
            sb.append(escapeForXML(course.getId()));
            sb.append("\" name=\"");
            sb.append(escapeForXML(course.getTitle()));
            sb.append("\" usertype=\"");
            sb.append("I");
            sb.append("\" created=\"");
            sb.append(course.getCreatedTime());
            sb.append("\" published=\"");
            sb.append(course.isPublished() ? "True" : "False");
            sb.append("\" />\n");
        }

        // close out
        sb.append("</coursemembership>\n");

        return sb.toString();
    }

    private static final String SCORE_KEY = "${SCORE}";

    public String encodeGradebook(Gradebook gradebook) {
        if (gradebook == null) {
            throw new IllegalArgumentException("gradebook must be set");
        }
        /*
         * SAMPLE <coursegradebook courseid="BFW61"> <user id="lm_student01" usertype="S"> <lineitem name="06/02/2009" pointspossible="50" type="iclicker polling scores" score="0"/> </user>
         * </coursegradebook>
         */

        // first make the map of lineitems strings and scores
        Map<String, String> lineitems = makeLineitemsMap(gradebook.getItems());
        Map<String, GradebookItemScore> studentGradeitemScores = makeGBItemScoresMap(gradebook.getItems());

        // make XML
        StringBuilder sb = new StringBuilder();
        // now make the outer course stuff
        sb.append("<coursegradebook courseid=\"");
        sb.append(escapeForXML(gradebook.getCourseId()));
        sb.append("\">\n");

        // loop through the students
        for (Student student : gradebook.getStudents()) {
            sb.append("  <user id=\"");
            sb.append(escapeForXML(student.getUsername()));
            sb.append("\" usertype=\"S\">\n");

            // put in the lineitems with scores
            for (Entry<String, String> entry : lineitems.entrySet()) {
                String scoreKey = student.getUserId() + entry.getKey();
                GradebookItemScore score = studentGradeitemScores.get(scoreKey);

                if (score != null) {
                    String lineitem = entry.getValue().replace(SCORE_KEY, score.getGrade());
                    sb.append("    ");
                    sb.append(lineitem);
                    sb.append("\n");
                }
            }

            // close student
            sb.append("  </user>\n");
        }

        // close out
        sb.append("</coursegradebook>\n");

        return sb.toString();
    }

    private Map<String, GradebookItemScore> makeGBItemScoresMap(List<GradebookItem> gradebookItems) {
        HashMap<String, GradebookItemScore> studentGradeitemScores = new HashMap<>();

        for (GradebookItem gbItem : gradebookItems) {
            // store the scores into a map as well
            for (GradebookItemScore score : gbItem.getScores()) {
                String key = score.getUserId() + gbItem.getName();
                studentGradeitemScores.put(key, score);
            }
        }

        return studentGradeitemScores;
    }

    private Map<String, String> makeLineitemsMap(List<GradebookItem> gradebookItems) {
        LinkedHashMap<String, String> lineitems = new LinkedHashMap<>();

        for (GradebookItem gbItem : gradebookItems) {
            lineitems.put(gbItem.getName(), "<lineitem name=\"" + escapeForXML(gbItem
                .getName()) + "\" pointspossible=\"" + (gbItem.getPointsPossible() == null ? "" : gbItem.getPointsPossible()) + "\" type=\"" + (gbItem.getType() == null ? "" : escapeForXML(gbItem
                .getType())) + "\" score=\"" + SCORE_KEY + "\"/>");
        }

        return lineitems;
    }

    public String encodeEnrollments(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("course must be set");
        }
        /*
         * SAMPLE <courseenrollment courseid="9ebcb080-02b6-43a9-8dc5-6aef890db579"> <user id="dbcc75e8-caeb-4e1d-b165-83402208da6e" usertype="S" firstname="Student" lastname="3333" emailid=""
         * uniqueid="stud3" clickerid="" whenadded="" /> <user id="1d7bc55c-4d84-4099-a8e9-821fad061dc8" usertype="S" firstname="Studnet" lastname="One" emailid="" uniqueid="stud1" clickerid=""
         * whenadded="" /> </courseenrollment>
         */

        StringBuilder sb = new StringBuilder();
        sb.append("<courseenrollment courseid=\"");
        sb.append(escapeForXML(course.getId()));
        sb.append("\">\n");

        // loop through students
        for (Student student : course.getStudents()) {
            // get the clicker data out first if there is any
            String[] cidsDates = makeClickerIdsAndDates(student.getClickerRegistrations());
            // now make the actual user data line
            sb.append("  <user id=\"");
            sb.append(student.getUserId());
            sb.append("\" usertype=\"");
            sb.append("S");
            sb.append("\" firstname=\"");
            sb.append(escapeForXML(student.getFname() == null ? "" : student.getFname()));
            sb.append("\" lastname=\"");
            sb.append(escapeForXML(student.getLname() == null ? "" : student.getLname()));
            sb.append("\" emailid=\"");
            sb.append(escapeForXML(student.getEmail() == null ? "" : student.getEmail()));
            sb.append("\" uniqueid=\"");
            sb.append(escapeForXML(student.getUsername()));
            sb.append("\" clickerid=\"");
            sb.append(escapeForXML(cidsDates[0]));
            sb.append("\" whenadded=\"");
            sb.append(escapeForXML(cidsDates[1]));
            sb.append("\" />\n");
        }

        // close out
        sb.append("</courseenrollment>\n");

        return sb.toString();
    }

    public Gradebook decodeGradebookXML(String xml) {
        /*
         * <coursegradebook courseid="BFW61"> <user id="lm_student01" usertype="S"> <lineitem name="06/02/2009" pointspossible="50" type="iclicker polling scores" score="0"/> </user> <user
         * id="lm_student02" usertype="S"> <lineitem name="06/02/2009" pointspossible="50" type="iclicker polling scores" score="0"/> </user> </coursegradebook>
         */

        if (StringUtils.isBlank(xml)) {
            throw new IllegalArgumentException("xml must be set");
        }

        // read the xml (try to anyway)
        DocumentBuilder db;

        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("XML parser failure: " + e, e);
        }

        Document doc;

        try {
            doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (SAXException e) {
            throw new RuntimeException("XML read failure: " + e, e);
        } catch (IOException e) {
            throw new RuntimeException("XML IO failure: " + e, e);
        }

        Gradebook gb;

        try {
            doc.getDocumentElement().normalize();
            // get the course id from the root attribute
            String courseId = doc.getDocumentElement().getAttribute("courseid");

            if (StringUtils.isBlank(courseId)) {
                throw new IllegalArgumentException("Invalid XML, no courseid in the root xml element");
            }

            NodeList users = doc.getElementsByTagName("user");

            if (users.getLength() == 0) {
                throw new IllegalArgumentException("Invalid XML, no user elements element");
            }

            gb = new Gradebook(courseId);
            gb.setCourseId(courseId);

            for (int i = 0; i < users.getLength(); i++) {
                Node userNode = users.item(i);

                if (userNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element user = (Element) userNode;
                    String userType = user.getAttribute("usertype");

                    if (!StringUtils.equalsIgnoreCase("s", userType)) {
                        continue; // skip this one
                    }

                    // valid user to process
                    String userId = user.getAttribute("id"); // this is the userId
                    if (StringUtils.isBlank(userId)) {
                        log.warn("Invalid XML for user, no id in the user element (skipping this entry): {}", user);
                        continue;
                    }

                    NodeList lineitems = user.getElementsByTagName("lineitem");

                    for (int j = 0; j < lineitems.getLength(); j++) {
                        Element lineitem = (Element) lineitems.item(j);
                        String liName = lineitem.getAttribute("name");

                        if (StringUtils.isBlank(liName)) {
                            throw new IllegalArgumentException("Invalid XML, no name in the lineitem xml element: " + lineitem);
                        }

                        String liType = lineitem.getAttribute("type");
                        Double liPointsPossible = 100.0;
                        String liPPText = lineitem.getAttribute("pointspossible");

                        if (StringUtils.isNotBlank(liPPText)) {
                            try {
                                liPointsPossible = Double.valueOf(liPPText);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid points possible ({}), using default of {}: {} ", liPPText, liPointsPossible, lineitem, e);
                            }
                        }

                        String liScore = lineitem.getAttribute("score");

                        if (StringUtils.isBlank(liScore)) {
                            log.warn("Invalid score ({}), skipping this entry: {}", liScore, lineitem);
                            continue;
                        }

                        GradebookItem gbi = new GradebookItem(gb.getId(), liName);

                        if (!gb.getItems().contains(gbi)) {
                            gbi.setPointsPossible(liPointsPossible);
                            gbi.setType(liType);
                            gb.getItems().add(gbi);
                        } else {
                            int pos = gb.getItems().lastIndexOf(gbi);

                            if (pos >= 0) {
                                gbi = gb.getItems().get(pos);
                            }
                        }
                        // add in the score
                        GradebookItemScore gbis = new GradebookItemScore(gbi.getName(), userId, liScore);
                        gbi.getScores().add(gbis);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid user node in XML: " + userNode);
                }
            }
        } catch (DOMException e) {
            throw new RuntimeException("XML DOM parsing failure: " + e, e);
        }
        return gb;
    }

    public String encodeSaveGradebookResults(String courseId, List<GradebookItem> items) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId must be set");
        }
        if (items == null) {
            throw new IllegalArgumentException("items must be set");
        }

        // check for any errors
        boolean hasErrors = false;

        for (GradebookItem gbItem : items) {
            if (gbItem.getScoreErrors() != null && !gbItem.getScoreErrors().isEmpty()) {
                hasErrors = true;
                break;
            }
        }

        /*
         * SAMPLE <errors courseid="BFW61"> <Userdoesnotexisterrors> <user id="student03" /> </Userdoesnotexisterrors> <Scoreupdateerrors> <user id="student02"> <lineitem name="Decsample"
         * pointspossible="0" type="Text" score="9" /> </user> </Scoreupdateerrors> <PointsPossibleupdateerrors> <user id="6367a431-557c-4869-88a7-229c2398f6ec"> <lineitem name="CMSIntTEST01"
         * pointspossible="50" type="iclicker polling scores" score="70" /> </user> </PointsPossibleupdateerrors> <Scoreupdateerrors> <user id="iclicker_student01"> <lineitem name="Mac-integrate-2"
         * pointspossible="31" type="092509Mac" score="13"/> </user> </Scoreupdateerrors> <Generalerrors> <user id="student02" error="CODE"> <lineitem name="itemName" pointspossible="35" score="XX"
         * error="CODE" /> </user> </Generalerrors> </errors>
         */

        String output = null;

        if (hasErrors) {
            Map<String, String> lineitems = makeLineitemsMap(items);
            HashSet<String> invalidUserIds = new HashSet<>();

            StringBuilder sb = new StringBuilder();
            sb.append("<errors courseId=\"");
            sb.append(escapeForXML(courseId));
            sb.append("\">\n");
            // loop through items and errors and generate errors xml blocks
            Map<String, StringBuilder> errorItems = new LinkedHashMap<>();

            for (GradebookItem gbItem : items) {
                if (gbItem.getScoreErrors() != null && !gbItem.getScoreErrors().isEmpty()) {
                    for (GradebookItemScore score : gbItem.getScores()) {
                        if (score.getError() != null) {
                            String lineitem = lineitems.get(gbItem.getName());

                            if (StringUtils.equals(AbstractExternalLogic.USER_DOES_NOT_EXIST_ERROR, score.getError())) {
                                String key = AbstractExternalLogic.USER_DOES_NOT_EXIST_ERROR;

                                if (invalidUserIds.add(score.getUserId())) {
                                    // only if the invalid user is not already listed in the errors
                                    if (!errorItems.containsKey(key)) {
                                        errorItems.put(key, new StringBuilder());
                                    }

                                    @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
                                    StringBuilder sbe = errorItems.get(key);
                                    sbe.append("    <user id=\"").append(score.getUserId()).append("\" />\n");
                                }
                            } else if (StringUtils.equals(AbstractExternalLogic.POINTS_POSSIBLE_UPDATE_ERRORS, score.getError())) {
                                String key = AbstractExternalLogic.POINTS_POSSIBLE_UPDATE_ERRORS;

                                if (!errorItems.containsKey(key)) {
                                    errorItems.put(key, new StringBuilder());
                                }

                                StringBuilder sbe = errorItems.get(key);
                                String li = lineitem.replace(SCORE_KEY, score.getGrade());
                                sbe.append("    <user id=\"").append(score.getUserId()).append("\">\n").append("      ").append(li).append("\n").append("    </user>\n");
                            } else if (StringUtils.equals(AbstractExternalLogic.SCORE_UPDATE_ERRORS, score.getError())) {
                                String key = AbstractExternalLogic.SCORE_UPDATE_ERRORS;

                                if (!errorItems.containsKey(key)) {
                                    errorItems.put(key, new StringBuilder());
                                }

                                StringBuilder sbe = errorItems.get(key);
                                String li = lineitem.replace(SCORE_KEY, score.getGrade());
                                sbe.append("    <user id=\"").append(score.getUserId()).append("\">\n").append("      ").append(li).append("\n").append("    </user>\n");
                            } else {
                                // general error
                                String key = AbstractExternalLogic.GENERAL_ERRORS;

                                if (!errorItems.containsKey(key)) {
                                    errorItems.put(key, new StringBuilder());
                                }

                                StringBuilder sbe = errorItems.get(key);
                                String li = lineitem.replace(SCORE_KEY, score.getGrade());
                                sbe.append("    <user id=\"").append(score.getUserId()).append("\" error=\"")
                                    .append(score.getError()).append("\">\n").append("      <error type=\"")
                                    .append(score.getError()).append("\" />\n").append("      ").append(li)
                                    .append("\n").append("    </user>\n");
                            }
                        }
                    }
                }
            }

            // loop through error items and dump to the output
            if (errorItems.containsKey(AbstractExternalLogic.USER_DOES_NOT_EXIST_ERROR)) {
                sb.append("  <Userdoesnotexisterrors>\n");
                sb.append(errorItems.get(AbstractExternalLogic.USER_DOES_NOT_EXIST_ERROR));
                sb.append("  </Userdoesnotexisterrors>\n");
            }

            if (errorItems.containsKey(AbstractExternalLogic.POINTS_POSSIBLE_UPDATE_ERRORS)) {
                sb.append("  <PointsPossibleupdateerrors>\n");
                sb.append(errorItems.get(AbstractExternalLogic.POINTS_POSSIBLE_UPDATE_ERRORS));
                sb.append("  </PointsPossibleupdateerrors>\n");
            }

            if (errorItems.containsKey(AbstractExternalLogic.SCORE_UPDATE_ERRORS)) {
                sb.append("  <Scoreupdateerrors>\n");
                sb.append(errorItems.get(AbstractExternalLogic.SCORE_UPDATE_ERRORS));
                sb.append("  </Scoreupdateerrors>\n");
            }

            if (errorItems.containsKey(AbstractExternalLogic.GENERAL_ERRORS)) {
                sb.append("  <Generalerrors>\n");
                sb.append(errorItems.get(AbstractExternalLogic.GENERAL_ERRORS));
                sb.append("  </Generalerrors>\n");
            }

            // close out
            sb.append("</errors>\n");
            output = sb.toString();
        }

        return output;
    }

    public String[] makeClickerIdsAndDates(Collection<ClickerRegistration> regs) {
        String clickerIds = "";
        String clickerAddedDates = "";

        if (regs != null) {
            DateFormat df = new SimpleDateFormat("MMM/dd/yyyy");
            StringBuilder cids = new StringBuilder();
            StringBuilder cads = new StringBuilder();
            int count = 0;

            for (ClickerRegistration registration : regs) {
                if (count > 0) {
                    cids.append(",");
                    cads.append(",");
                }

                String clickerId = registration.getClickerId();
                String clickerDate = df.format(registration.getDateCreated());
                cids.append(clickerId);
                cads.append(clickerDate);
                count++;

                if (!disableAlternateRemoteID) {
                    // add in the alternate clicker id if needed
                    String alternateId = translateClickerId(clickerId);

                    if (alternateId != null) {
                        cids.append(",");
                        cads.append(",");
                        cids.append(alternateId);
                        cads.append(clickerDate);
                        count++;
                    }
                }
            }

            clickerIds = cids.toString();
            clickerAddedDates = cads.toString();
        }

        return new String[] {clickerIds, clickerAddedDates};
    }

    /*
     * ************************************************************************ Clicker ID validation ************************************************************************
     */

    ThreadLocal<String> lastValidGOKey = new ThreadLocal<>();

    public static final String CLICKERID_SAMPLE = "11A4C277";

    /**
     * Cleans up and validates a given clickerId
     * 
     * @param clickerId a remote clicker ID
     * @return the cleaned up and valid clicker ID
     * @throws ClickerIdInvalidException if the id is invalid for some reason, the exception will indicate the type of validation failure
     */
    public String validateClickerId(String clickerId) {
        return validateClickerId(clickerId, null);
    }

    /**
     * Cleans up and validates a given clickerId
     * 
     * @param clickerId a remote clicker ID
     * @param lastName OPTIONAL user lastname (only used for GO checks), defaults to current user
     * @return the cleaned up and valid clicker ID
     * @throws ClickerIdInvalidException if the id is invalid for some reason, the exception will indicate the type of validation failure
     */
    public String validateClickerId(String clickerId, String lastName) {
        if (StringUtils.isBlank(clickerId)) {
            throw new ClickerIdInvalidException("empty or null clickerId", Failure.EMPTY, clickerId);
        }
        if (StringUtils.length(clickerId) > CLICKERID_LENGTH) {
            throw new ClickerIdInvalidException("Clicker ID: '" + clickerId + "' length cannot be greater than " + CLICKERID_LENGTH, Failure.LENGTH, clickerId);
        }

        int clickerIdLength = clickerId.length();

        if (clickerIdLength <= CLICKERID_LENGTH) {
            // remote ids
            clickerId = clickerId.trim().toUpperCase();

            if (!clickerId.matches("[0-9A-F]+")) {
                throw new ClickerIdInvalidException("clickerId can only contains A-F and 0-9", Failure.CHARS, clickerId);
            }

            while (clickerId.length() < CLICKERID_LENGTH) {
                clickerId = "0" + clickerId;
            }

            String[] idArray = new String[4];
            idArray[0] = clickerId.substring(0, 2);
            idArray[1] = clickerId.substring(2, 4);
            idArray[2] = clickerId.substring(4, 6);
            idArray[3] = clickerId.substring(6, 8);
            int checksum = 0;

            for (String piece : idArray) {
                int hex = Integer.parseInt(piece, 16);
                checksum = checksum ^ hex;
            }

            if (checksum != 0) {
                throw new ClickerIdInvalidException("clickerId checksum (" + checksum + ") validation failed", Failure.CHECKSUM, clickerId);
            }
        } else {
            // totally invalid clicker length
            this.lastValidGOKey.remove();
            throw new ClickerIdInvalidException("clicker_id is an invalid length (" + clickerIdLength + "), must be less than or equal to " + CLICKERID_LENGTH + " chars", Failure.LENGTH, clickerId);
        }

        if (StringUtils.equals(CLICKERID_SAMPLE, clickerId)) {
            throw new ClickerIdInvalidException("clickerId cannot match the sample ID", Failure.SAMPLE, clickerId);
        }

        return clickerId;
    }

    /**
     * For all remoteids starting with 2, 4, 8, we have to generate an alternate id and concatenate it with the existing remote ids for that particular user in the data sent to the iclicker desktop
     * app (this is like creating an extra clickerid based on the existing ones)
     * 
     * @param clickerId a remote clicker ID
     * @return a translated clicker ID OR null if no translation is required or id is invalid
     */
    public String translateClickerId(String clickerId) {
        String alternateId = null;

        try {
            // validate the input, do nothing but return null if invalid
            clickerId = validateClickerId(clickerId, null);
            if (clickerId.length() == CLICKERID_LENGTH) {
                char startsWith = clickerId.charAt(0);
                if ('2' == startsWith || '4' == startsWith || '8' == startsWith) {
                    // found clicker to translate
                    int p1 = Integer.parseInt("0" + clickerId.charAt(1), 16);
                    int p2 = Integer.parseInt(clickerId.substring(2, 4), 16);
                    int p3 = Integer.parseInt(clickerId.substring(4, 6), 16);
                    int p4 = p1 ^ p2 ^ p3;
                    String part4 = Integer.toHexString(p4).toUpperCase();

                    if (part4.length() == 1) {
                        part4 = "0" + part4;
                    }

                    alternateId = "0" + clickerId.substring(1, 6) + part4;
                }
            }
        } catch (ClickerIdInvalidException e) {
            alternateId = null;
        }

        return alternateId;
    }

    /**
     * Attempt to decode the XML into readable values in a map
     *
     * @param xml XML
     * @return the map of attributes from S student record
     * @throws IllegalArgumentException if the xml is invalid or blank
     * @throws RuntimeException if there is an internal failure in the XML parser
     */
    public Map<String, String> decodeGetRegisteredForClickerMACResult(String xml) {
        /*
         * <StudentEnrol> <S StudentId="testgoqait99" FirstName="testgoqait99" LastName="testgoqait99" MiddleName="" WebClickerId="C570BF0C2154"/> </StudentEnrol>
         */

        if (StringUtils.isBlank(xml)) {
            throw new IllegalArgumentException("xml must be set");
        }

        // read the xml (try to anyway)
        DocumentBuilder db;

        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("XML parser failure: " + e, e);
        }

        Document doc;

        try {
            doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (SAXException e) {
            throw new RuntimeException("XML read failure: " + e, e);
        } catch (IOException e) {
            throw new RuntimeException("XML IO failure: " + e, e);
        }

        HashMap<String, String> m = new HashMap<>();

        try {
            doc.getDocumentElement().normalize();
            NodeList users = doc.getElementsByTagName("S");

            if (users.getLength() == 0) {
                throw new IllegalArgumentException("Invalid XML, no S element");
            }

            Node userNode = users.item(0);

            if (userNode.getNodeType() == Node.ELEMENT_NODE) {
                Element user = (Element) userNode;
                NamedNodeMap attributes = user.getAttributes();

                for (int j = 0; j < attributes.getLength(); j++) {
                    String name = attributes.item(j).getNodeName();
                    String value = attributes.item(j).getNodeValue();
                    m.put(name, value);
                }
            } else {
                throw new IllegalArgumentException("Invalid user node in XML: " + userNode);
            }
        } catch (DOMException e) {
            throw new RuntimeException("XML DOM parsing failure: " + e, e);
        }
        return m;
    }

    public static final char AMP = '&';
    public static final char APOS = '\'';
    public static final char GT = '>';
    public static final char LT = '<';
    public static final char QUOT = '"';

    /**
     * Escape a string for XML encoding: replace special characters with XML escapes <br/>
     * &, <, >, ", ' will be escaped
     * 
     * @param string The string to be escaped.
     * @return The escaped string.
     */
    public static String escapeForXML(String string) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);

            switch (c) {
                case AMP:
                    sb.append("&amp;");
                    break;
                case LT:
                    sb.append("&lt;");
                    break;
                case GT:
                    sb.append("&gt;");
                    break;
                case QUOT:
                    sb.append("&quot;");
                    break;
                case APOS:
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Validates the clicker registration list, removing invalid ones
     *
     * @param clickerRegistrations
     * @return
     */
    private List<ClickerRegistration> removeInvalidClickerRegistrations(List<ClickerRegistration> clickerRegistrations) {
     // validate the clicker registration IDs
        Iterator<ClickerRegistration> i = clickerRegistrations.iterator();
        while (i.hasNext()) {
            ClickerRegistration clickerRegistration = i.next();
            if (!isValidClickerRegistration(clickerRegistration)) {
                //invalid registration... remove it from the list
                i.remove();
            }
        }

        return clickerRegistrations;
    }

    /**
     * Validates the clicker registration:
     *
     * 1. Clicker ID must be 8 characters
     *
     * @param clickerRegistration
     * @return
     */
    private boolean isValidClickerRegistration(ClickerRegistration clickerRegistration) {
        // clicker ID length is not less than or equal to 8 characters, so it is invalid
        return StringUtils.length(clickerRegistration.getClickerId()) <= CLICKERID_LENGTH;
    }

    public static IClickerLogic getInstance() {
        return IClickerLogic.instance;
    }

    protected static void setInstance(IClickerLogic instance) {
        IClickerLogic.instance = instance;
    }

}
