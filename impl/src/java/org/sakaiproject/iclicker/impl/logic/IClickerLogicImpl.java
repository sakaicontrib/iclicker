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
package org.sakaiproject.iclicker.impl.logic;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.genericdao.api.search.Order;
import org.sakaiproject.genericdao.api.search.Restriction;
import org.sakaiproject.genericdao.api.search.Search;
import org.sakaiproject.iclicker.exception.ClickerIdInvalidException;
import org.sakaiproject.iclicker.exception.ClickerLockException;
import org.sakaiproject.iclicker.exception.ClickerRegisteredException;
import org.sakaiproject.iclicker.exception.ClickerIdInvalidException.Failure;
import org.sakaiproject.iclicker.impl.dao.IClickerDao;
import org.sakaiproject.iclicker.model.Course;
import org.sakaiproject.iclicker.model.Gradebook;
import org.sakaiproject.iclicker.model.GradebookItem;
import org.sakaiproject.iclicker.model.GradebookItemScore;
import org.sakaiproject.iclicker.model.Student;
import org.sakaiproject.iclicker.model.User;
import org.sakaiproject.iclicker.model.dao.ClickerRegistration;
import org.sakaiproject.iclicker.model.dao.ClickerUserKey;
import org.sakaiproject.iclicker.service.BigRunner;
import org.sakaiproject.iclicker.service.IClickerLogic;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DelegatingMessageSource;
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
import lombok.extern.slf4j.Slf4j;

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

/**
 * This is the implementation of the business logic interface,
 * this handles all the business logic and processing for the application.
 */
@Slf4j
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class IClickerLogicImpl implements IClickerLogic {

    // CONFIG
    public static final String DEFAULT_SERVER_URL = "http://localhost/sakai";
    public String serverId = "UNKNOWN_SERVER_ID";
    public String serverURL = DEFAULT_SERVER_URL;
    @Getter public String domainURL = DEFAULT_SERVER_URL;
    @Getter public String workspacePageTitle = "i>clicker";
    private static final String OWNER_ID = "ownerId";
    private static final String SCORE_KEY = "${SCORE}";
    public boolean disableAlternateRemoteID = false;
    @Getter public boolean forceRestDebugging = false;
    public ThreadLocal<String> lastValidGOKey = new ThreadLocal<>();
    public static final String CLICKERID_SAMPLE = "11A4C277";
    private String notifyEmailsString = null;
    private String[] notifyEmails = null;
    @Getter private boolean singleSignOnHandling = false;
    private String singleSignOnSharedkey = null;
    private int maxCoursesForInstructor = 100;
    private static final int CLICKERID_LENGTH = 8;
    @Getter private List<String> failures = new ArrayList<>();
    public static final char AMP = '&';
    public static final char APOS = '\'';
    public static final char GT = '>';
    public static final char LT = '<';
    public static final char QUOT = '"';

    /**
     * Special tracker to see if the system is already running a thread,
     * this is meant to ensure that more than one large scale operation is not running at once.
     */
    private WeakReference<BigRunner> runnerHolder;
    private static final String ICLICKER_TOOL_ID = "sakai.iclicker";

    @Setter private IClickerDao dao;
    @Setter @Getter private ExternalLogicImpl externalLogic;
    @Setter private DelegatingMessageSource messageSource;
    protected static IClickerLogicImpl instance;

    @Override
    public void init() {
        // store this so we can get the service later
        IClickerLogicImpl.setInstance(this);
        serverURL = externalLogic.getConfigurationSetting(ExternalLogicImpl.SETTING_SERVER_URL, DEFAULT_SERVER_URL);
        domainURL = serverURL;
        workspacePageTitle = externalLogic.getConfigurationSetting("iclicker.workspace.title", workspacePageTitle);
        disableAlternateRemoteID = externalLogic.getConfigurationSetting(
            "iclicker.turn.off.alternate.remote.id",
            disableAlternateRemoteID
        );
        serverId = externalLogic.getConfigurationSetting(AbstractExternalLogic.SETTING_SERVER_ID, serverId);
        forceRestDebugging = externalLogic.getConfigurationSetting("iclicker.rest.debug", false);
        maxCoursesForInstructor = externalLogic.getConfigurationSetting(
            "iclicker.max.courses",
            maxCoursesForInstructor
        );

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
                log.warn(
                    "Invalid list of email addresses in iclicker.notify.emails config setting: {}",
                    notifyEmailsString
                );
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
    @Override
    public boolean isSingleSignOnEnabled() {
        return singleSignOnHandling;
    }

    @Override
    public void sendNotification(String message, Exception failure) {
        String body = "i>clicker Sakai integrate plugin notification (" + new Date() + ")\n" + message + "\n";

        if (failure != null) {
            // get the stacktrace out
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            failure.printStackTrace(pw);
            String stacktrace = new StringBuilder("Full stacktrace:\n")
                .append(failure.getClass().getSimpleName())
                .append(":")
                .append(failure.getMessage())
                .append(":\n")
                .append(sw.toString())
                .toString();
            body = body +
                    (new StringBuilder("\nFailure:\n")
                        .append(failure.toString())
                        .append("\n\n")
                        .append(stacktrace)
                    ).toString();

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

    @Override
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

    @Override
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

    @Override
    public String makeUserKey(String userId, boolean makeNew) {
        if (userId == null) {
            userId = externalLogic.getCurrentUserId();
        }
        if (userId == null) {
            throw new IllegalStateException("no current user, cannot generate a user key");
        }

        if (!externalLogic.isUserInstructor(userId) && !externalLogic.isUserAdmin(userId)) {
            // if user is not an instructor or an admin then we will not make a key for them,
            // this is to block students from getting a pass key
            throw new IllegalStateException(
                String.format("current user (%s) is not an instructor, cannot generate user key for them", userId)
            );
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
                // this should not have happened but it means the key already exists somehow,
                // probably a sync issue of some kind
                log.warn("Failed when attempting to create a new clicker user key for: {}", userId);
            }
        }

        return cuk.getUserKey();
    }

    @Override
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

    @Override
    public void setSharedKey(String sharedKey) {
        if (StringUtils.isEmpty(sharedKey)) {
            return;
        }

        if (sharedKey.length() < 10) {
            log.warn(
                "i>clicker shared key ({}) is too short, must be at least 10 chars long. " +
                "SSO shared key will be ignored until a longer key is entered.",
                sharedKey
            );
        } else {
            singleSignOnHandling = true;
            singleSignOnSharedkey = sharedKey;
            log.info(
                "i>clicker plugin SSO handling enabled by shared key, " +
                "note that this will disable normal username/password handling"
            );
        }
    }

    @Override
    public String getSharedKey() {
        if (singleSignOnHandling && externalLogic.isUserAdmin(externalLogic.getCurrentUserId())) {
            return singleSignOnSharedkey;
        }

        return "";
    }

    @Override
    public boolean verifyKey(String key) {
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("key must be set in order to verify the key");
        }

        if (!singleSignOnHandling) {
            return false;
        }

        String invalidKey = "i>clicker shared key (" + key + ") format is invalid ";

        // encoding process requires the key and timestamp so split them from the passed in key
        int splitIndex = key.lastIndexOf('|');

        if ((splitIndex == -1) || (key.length() < splitIndex + 1)) {
            throw new IllegalArgumentException(
                String.format("%s (no |), must be {encoded key}|{timestamp}", invalidKey)
            );
        }

        String actualKey = key.substring(0, splitIndex);

        if (StringUtils.isEmpty(actualKey)) {
            throw new IllegalArgumentException(
                String.format("%s (missing encoded key), must be {encoded key}|{timestamp}", invalidKey)
            );
        }

        String timestampStr = key.substring(splitIndex + 1);

        if (StringUtils.isEmpty(timestampStr)) {
            throw new IllegalArgumentException(
                String.format("%s (missing timestamp), must be {encoded key}|{timestamp}", invalidKey)
            );
        }

        long timestamp;

        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("%s (non numeric timestamp), must be {encoded key}|{timestamp}", invalidKey)
            );
        }

        // check this key is still good (must be within 5 mins of now)
        long unixTime = System.currentTimeMillis() / 1000L;
        long timeDiff = Math.abs(timestamp - unixTime);

        if (timeDiff > 300L) {
            throw new SecurityException(
                String.format(
                    "%s, this timestamp (%s) is more than 5 minutes different from the current time (%s)",
                    new Object[] {invalidKey, timestamp, unixTime}
                )
            );
        }

        // finally we verify the key with the one in the config
        byte[] sha1Bytes = DigestUtils.sha1(singleSignOnSharedkey + ":" + timestamp);
        String sha1Hex = Hex.encodeHexString(sha1Bytes);

        if (!StringUtils.equals(actualKey, sha1Hex)) {
            throw new SecurityException(
                String.format(
                    "%s, does not match with the key (%s) in Sakai (using timestamp: %s)",
                    new Object[] {invalidKey, sha1Hex, timestamp}
                )
            );
        }

        return true;
    }

    // *******************************************************************************
    // Admin i>clicker tool in workspace handling

    @Override
    public BigRunner startRunnerOperation(String type) {
        BigRunner runner = getRunnerStatus();

        if (runner != null) {
            // allow the runner to be cleared if done
            if (runner.isComplete()) {
                runner = null;
            } else {
                if (!StringUtils.equals(type, runner.getType())) {
                    throw new IllegalStateException(
                        String.format("Already running a big runner of a different type: %s", runner.getType())
                    );
                }
            }
        }

        if (runner == null) {
            // try to obtain a lock
            Boolean gotLock = dao.obtainLock(BigRunner.RUNNER_LOCK, serverId, 600000); // expire 10 mins

            if (gotLock != null && gotLock) {
                if (BigRunner.RUNNER_TYPE_ADD.equalsIgnoreCase(type)) {
                    runner = getExternalLogic().makeAddToolToWorkspacesRunner(
                        ICLICKER_TOOL_ID,
                        workspacePageTitle,
                        null
                    );
                } else if (BigRunner.RUNNER_TYPE_REMOVE.equalsIgnoreCase(type)) {
                    runner = getExternalLogic().makeRemoveToolFromWorkspacesRunner(ICLICKER_TOOL_ID);
                } else {
                    throw new IllegalArgumentException(String.format("Unknown type of runner operation: %s", type));
                }

                final BigRunner bigRunner = runner;
                Runnable runnable = new Runnable() {
                    public void run() {
                        try {
                            bigRunner.run();
                        } catch (Exception e) {
                            String msg = String.format("long running process (%s) failure: %s", bigRunner, e);
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
                String msg = String.format(
                    "Could not obtain a lock (%s) on server (%s): ",
                    new Object[] {BigRunner.RUNNER_LOCK, serverId, gotLock}
                );
                log.info(msg);
                throw new ClickerLockException(msg, BigRunner.RUNNER_LOCK, serverId);
            }
        }

        return runner;
    }

    @Override
    public BigRunner getRunnerStatus() {
        return this.runnerHolder != null ? this.runnerHolder.get() : null;
    }

    /**
     * clears the holder.
     */
    @Override
    public void clearRunner() {
        if (runnerHolder != null) {
            runnerHolder.clear();
            runnerHolder = null;
        }
    }

    // *******************************************************************************
    // Clicker registration handling

    @Override
    public ClickerRegistration getItemById(Long id) {
        ClickerRegistration item = dao.findById(ClickerRegistration.class, id);

        if (item != null) {
            if (!canReadItem(item, externalLogic.getCurrentUserId())) {
                throw new SecurityException(
                    String.format(
                        "User (%s) not allowed to access registration (%s)",
                        externalLogic.getCurrentUserId(),
                        item
                    )
                );
            }
        }

        return item;
    }

    @Override
    public ClickerRegistration getItemByClickerId(String clickerId) {
        return getItemByClickerId(clickerId, null);
    }

    @Override
    public ClickerRegistration getItemByClickerId(String clickerId, String ownerId) {
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

        ClickerRegistration item = dao.findOneBySearch(
            ClickerRegistration.class,
            new Search(
                new Restriction[] {
                    new Restriction("clickerId", clickerId),
                    new Restriction(OWNER_ID, userId)
                }
            )
        );
        if (item != null) {
            if (!canReadItem(item, externalLogic.getCurrentUserId())) {
                throw new SecurityException(
                    String.format(
                        "User (%s) not allowed to access registration (%s)",
                        externalLogic.getCurrentUserId(),
                        item
                    )
                );
            }
            if (!isValidClickerRegistration(item)) {
                // invalid registration, don't return it
                return null;
            }
        }

        return item;
    }

    @Override
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

    @Override
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

    @Override
    public List<ClickerRegistration> getAllVisibleItems(String userId, String locationId) {
        List<ClickerRegistration> l;

        if (locationId == null) {
            // get the items for this user only
            l = dao.findBySearch(ClickerRegistration.class, new Search(new Restriction[] {new Restriction(OWNER_ID,
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
                l = dao.findBySearch(ClickerRegistration.class, new Search(new Restriction[] {new Restriction(OWNER_ID,
                                userId), new Restriction("activated", true)}));
            }
        }

        return l;
    }

    @Override
    public List<ClickerRegistration> getAllItems(
            int first, int max, String order, String searchStr, boolean includeUserDisplayNames) {
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

    @Override
    public int countAllItems() {
        return dao.countAll(ClickerRegistration.class);
    }

    @Override
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

    @Override
    public void removeItem(ClickerRegistration item) {
        log.debug("In removeItem with item: {}", item);

        // check if current user can remove this item
        if (externalLogic.isUserAdmin(externalLogic.getCurrentUserId())) {
            dao.delete(item);
            log.info("Removing clicker registration: {}", item);
        } else {
            throw new SecurityException(
                String.format(
                    "Uuser cannot remove registration %s because they do not have permission, only admins can remove",
                    item
                )
            );
        }
    }

    @Override
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
                throw new IllegalArgumentException(
                    String.format(
                        "user id (%s) is invalid (cannot match to user)",
                        item.getOwnerId()
                    )
                );
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
            throw new SecurityException(
                String.format(
                    "Current user cannot update item %s because they do not have permission",
                    item.getId()
                )
            );
        }
    }

    @Override
    public ClickerRegistration createRegistration(String clickerId) {
        return createRegistration(clickerId, null);
    }

    @Override
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

    @Override
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

    @Override
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

                search.addRestriction(new Restriction(OWNER_ID, owners));
            }

            search.addOrder(new Order(OWNER_ID));
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

    @Override
    public List<ClickerRegistration> getClickerRegistrationsForUser(String userId, boolean activeOnly) {
        Search search = new Search();

        if (activeOnly) {
            search.addRestriction(new Restriction("activated", true)); // only active ones
        }

        search.addRestriction(new Restriction(OWNER_ID, userId));
        search.addOrder(new Order("dateCreated", false));

        return dao.findBySearch(ClickerRegistration.class, search);
    }

    @Override
    public List<Course> getCoursesForInstructorWithStudents(String courseId) {
        List<Course> courses = externalLogic.getCoursesForInstructor(courseId, maxCoursesForInstructor);

        if (courseId != null && courses.size() == 1) {
            // add in the students
            Course c = courses.get(0);
            c.setStudents(getStudentsForCourseWithClickerReg(c.getId()));
        }

        return courses;
    }

    @Override
    public String getCourseTitle(String courseId) {
        return externalLogic.getLocationTitle(courseId);
    }

    @Override
    public Gradebook getCourseGradebook(String courseId, String gbItemName) {
        Gradebook gb = externalLogic.getCourseGradebook(courseId, gbItemName);
        gb.setStudents(getStudentsForCourseWithClickerReg(courseId));

        return gb;
    }

    /**
     * Save a gradebook item and optionally the scores within <br/>
     * Scores must have at least the studentId or username AND the grade set.
     * 
     * @param gbItem the gradebook item to save, must have at least the gradebookId and name set
     * @return the updated gradebook item and scores, contains any errors that occurred
     */
    @Override
    public GradebookItem saveGradebookItem(GradebookItem gbItem) {
        return externalLogic.saveGradebookItem(gbItem);
    }

    /**
     * Save a gradebook (saves all items in the gradebook).
     * 
     * @param gb the gradebook to save
     * @return the updated gradebook items and scores, contains any errors that occurred
     */
    @Override
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

    @Override
    public String encodeClickerRegistration(ClickerRegistration registration) {
        if (registration == null) {
            throw new IllegalArgumentException("registration must be set");
        }
        /*
         * SAMPLE <Register> <S FirstName="Tim" LastName="Stelzer" StudentID="tstelzer"
         * URL="http://www.iclicker.com/" clickerID="11111111" Enabled="True"></S> </Register>
         */
        User user = externalLogic.getUser(registration.getOwnerId());

        if (user == null) {
            throw new IllegalStateException(
                String.format(
                    "Could not get info about the user (%s) related to this clicker registration",
                    registration.getOwnerId()
                )
            );
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
     * Encode response from registration of clicker data This option should be available where the instructor
     * already has the clicker registration file (Remoteid.csv) and wants to upload the
     * registration(s) to the CMS Server.
     * 
     * @param registrations the registrations resulting from the action
     * @param status true if new registration, false otherwise
     * @param message the human readable message
     * @return the encoded XML
     * @throws IllegalStateException if the user cannot be found
     * @throws IllegalArgumentException if the data is invalid
     */
    @Override
    public String encodeClickerRegistrationResult(
            List<ClickerRegistration> registrations, boolean status, String message) {
        if (registrations == null || registrations.isEmpty()) {
            throw new IllegalArgumentException("registrations must be set");
        }
        if (message == null) {
            throw new IllegalArgumentException("message must be set");
        }
        /*
         * SAMPLE
         * 1) When clicker is already registered to some one else - the same message should be returned
         * that is displayed in the plug-in in xml format <RetStatus Status="False" Message=""/>
         * 2) When clicker is already registered to the same user - the same message should be returned that is
         * displayed in the plug-in in xml format. <RetStatus Status="False" Message=""/>
         * 3) When studentid is not found in the CMS <RetStatus Status="False" Message="Student not found in the CMS"/>
         * 4) Successful registration - <RetStatus Status="True" Message="..."/>
         */

        return "<RetStatus Status=\"" + (status ? "True" : "False") + "\" Message=\"" + escapeForXML(message) + "\" />";
    }

    @Override
    public ClickerRegistration decodeClickerRegistration(String xml) {
        /*
         * <Register> <S DisplayName="DisplayName-azeckoski-123456" FirstName="First"
         * LastName="Lastazeckoski-123456" StudentID="eid-azeckoski-123456" Email="azeckoski-123456@email.com"
         * URL="http://sakaiproject.org"; ClickerID="11111111"></S> </Register>.
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
                    throw new IllegalArgumentException(
                        "Invalid XML for registration, no id in the ClickerID element (Cannot process)"
                    );
                }

                String userId = user.getAttribute("StudentID"); // this is the userId

                if (StringUtils.isBlank(userId)) {
                    throw new IllegalArgumentException(
                        "Invalid XML for registration, no id in the StudentID element (Cannot process)"
                    );
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

    @Override
    public String encodeCourses(String instructorId, List<Course> courses) {
        if (courses == null) {
            throw new IllegalArgumentException("courses must be set");
        }
        /*
         * SAMPLE <coursemembership username="test_instructor01"> <course id="BFW61" name="BFW - iClicker Test"
         * created="111111111" published="true" usertype="I" /> </coursemembership>
         */

        User user = externalLogic.getUser(instructorId);

        if (user == null) {
            throw new IllegalStateException(
                String.format(
                    "Could not get info about the user (%s) related to the course listing",
                    instructorId
                )
            );
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

    @Override
    public String encodeGradebook(Gradebook gradebook) {
        if (gradebook == null) {
            throw new IllegalArgumentException("gradebook must be set");
        }
        /*
         * SAMPLE <coursegradebook courseid="BFW61"> <user id="lm_student01" usertype="S"> <lineitem name="06/02/2009"
         * pointspossible="50" type="iclicker polling scores" score="0"/> </user>
         * </coursegradebook>.
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
            lineitems.put(gbItem.getName(), "<lineitem name=\"" +
                escapeForXML(
                    gbItem.getName()
                ) +
                "\" pointspossible=\"" +
                (gbItem.getPointsPossible() == null ? "" : gbItem.getPointsPossible()) +
                "\" type=\"" +
                (gbItem.getType() == null ? "" : escapeForXML(gbItem.getType())) +
                "\" score=\"" +
                SCORE_KEY +
                "\"/>"
            );
        }

        return lineitems;
    }

    @Override
    public String encodeEnrollments(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("course must be set");
        }
        /*
         * SAMPLE <courseenrollment courseid="9ebcb080-02b6-43a9-8dc5-6aef890db579">
         * <user id="dbcc75e8-caeb-4e1d-b165-83402208da6e" usertype="S" firstname="Student" lastname="3333" emailid=""
         * uniqueid="stud3" clickerid="" whenadded="" /> <user id="1d7bc55c-4d84-4099-a8e9-821fad061dc8"
         * usertype="S" firstname="Student" lastname="One" emailid="" uniqueid="stud1" clickerid=""
         * whenadded="" /> </courseenrollment>.
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

    @Override
    public Gradebook decodeGradebookXML(String xml) {
        /*
         * <coursegradebook courseid="BFW61"> <user id="lm_student01" usertype="S">
         * <lineitem name="06/02/2009" pointspossible="50" type="iclicker polling scores" score="0"/> </user> <user
         * id="lm_student02" usertype="S"> <lineitem name="06/02/2009" pointspossible="50"
         * type="iclicker polling scores" score="0"/> </user> </coursegradebook>.
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
                            throw new IllegalArgumentException(
                                String.format(
                                    "Invalid XML, no name in the lineitem xml element: %s",
                                    lineitem
                                )
                            );
                        }

                        Double liPointsPossible = 100.0;
                        String liPPText = lineitem.getAttribute("pointspossible");

                        if (StringUtils.isNotBlank(liPPText)) {
                            try {
                                liPointsPossible = Double.valueOf(liPPText);
                            } catch (NumberFormatException e) {
                                log.warn(
                                    "Invalid points possible ({}), using default of {}: {} ",
                                    liPPText,
                                    liPointsPossible,
                                    lineitem,
                                    e
                                );
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
                            gbi.setType(null);
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

    @Override
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
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
         * SAMPLE <errors courseid="BFW61"> <Userdoesnotexisterrors> <user id="student03" />
         * </Userdoesnotexisterrors> <Scoreupdateerrors> <user id="student02"> <lineitem name="Decsample"
         * pointspossible="0" type="Text" score="9" /> </user> </Scoreupdateerrors>
         * <PointsPossibleupdateerrors> <user id="6367a431-557c-4869-88a7-229c2398f6ec"> <lineitem name="CMSIntTEST01"
         * pointspossible="50" type="iclicker polling scores" score="70" /> </user>
         * </PointsPossibleupdateerrors> <Scoreupdateerrors> <user id="iclicker_student01">
         * <lineitem name="Mac-integrate-2"
         * pointspossible="31" type="092509Mac" score="13"/> </user> </Scoreupdateerrors>
         * <Generalerrors> <user id="student02" error="CODE"> <lineitem name="itemName" pointspossible="35" score="XX"
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
                                sbe.append("    <user id=\"")
                                    .append(score.getUserId())
                                    .append("\">\n")
                                    .append("      ")
                                    .append(li)
                                    .append("\n")
                                    .append("    </user>\n");
                            } else if (StringUtils.equals(AbstractExternalLogic.SCORE_UPDATE_ERRORS, score.getError())) {
                                String key = AbstractExternalLogic.SCORE_UPDATE_ERRORS;

                                if (!errorItems.containsKey(key)) {
                                    errorItems.put(key, new StringBuilder());
                                }

                                StringBuilder sbe = errorItems.get(key);
                                String li = lineitem.replace(SCORE_KEY, score.getGrade());
                                sbe.append("    <user id=\"")
                                    .append(score.getUserId())
                                    .append("\">\n")
                                    .append("      ")
                                    .append(li)
                                    .append("\n")
                                    .append("    </user>\n");
                            } else {
                                // general error
                                String key = AbstractExternalLogic.GENERAL_ERRORS;

                                if (!errorItems.containsKey(key)) {
                                    errorItems.put(key, new StringBuilder());
                                }

                                StringBuilder sbe = errorItems.get(key);
                                String li = lineitem.replace(SCORE_KEY, score.getGrade());
                                sbe.append("    <user id=\"")
                                    .append(score.getUserId())
                                    .append("\" error=\"")
                                    .append(score.getError())
                                    .append("\">\n")
                                    .append("      <error type=\"")
                                    .append(score.getError())
                                    .append("\" />\n")
                                    .append("      ")
                                    .append(li)
                                    .append("\n")
                                    .append("    </user>\n");
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

    @Override
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
     * ************************************************************************
     * Clicker ID validation
     * ************************************************************************
     */

    @Override
    public String validateClickerId(String clickerId) {
        return validateClickerId(clickerId, null);
    }

    @Override
    public String validateClickerId(String clickerId, String lastName) {
        if (StringUtils.isBlank(clickerId)) {
            throw new ClickerIdInvalidException("empty or null clickerId", Failure.EMPTY, clickerId);
        }
        if (StringUtils.length(clickerId) > CLICKERID_LENGTH) {
            throw new ClickerIdInvalidException(
                String.format(
                    "Clicker ID: '%s' length cannot be greater than %s",
                    clickerId,
                    CLICKERID_LENGTH
                ),
                Failure.LENGTH,
                clickerId
            );
        }

        int clickerIdLength = clickerId.length();

        if (clickerIdLength <= CLICKERID_LENGTH) {
            // remote ids
            clickerId = clickerId.trim().toUpperCase();

            if (!clickerId.matches("[0-9A-F]+")) {
                throw new ClickerIdInvalidException(
                    "clickerId can only contains A-F and 0-9",
                    Failure.CHARS,
                    clickerId
                );
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
                throw new ClickerIdInvalidException(
                    String.format("clickerId checksum (%s) validation failed",
                        checksum
                    ),
                    Failure.CHECKSUM,
                    clickerId
                );
            }
        } else {
            // totally invalid clicker length
            this.lastValidGOKey.remove();
            throw new ClickerIdInvalidException(
                String.format(
                    "clicker_id is an invalid length (%s), must be less than or equal to %s chars",
                    clickerIdLength,
                    CLICKERID_LENGTH
                ),
                Failure.LENGTH,
                clickerId
            );
        }

        if (StringUtils.equals(CLICKERID_SAMPLE, clickerId)) {
            throw new ClickerIdInvalidException("clickerId cannot match the sample ID", Failure.SAMPLE, clickerId);
        }

        return clickerId;
    }

    @Override
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

    @Override
    public Map<String, String> decodeGetRegisteredForClickerMACResult(String xml) {
        /*
         * <StudentEnrol> <S StudentId="testgoqait99" FirstName="testgoqait99" LastName="testgoqait99"
         * MiddleName="" WebClickerId="C570BF0C2154"/> </StudentEnrol>
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
                    break;
            }
        }

        return sb.toString();
    }

    private List<ClickerRegistration> removeInvalidClickerRegistrations(
            List<ClickerRegistration> clickerRegistrations) {
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

    private boolean isValidClickerRegistration(ClickerRegistration clickerRegistration) {
        // clicker ID length is not less than or equal to 8 characters, so it is invalid
        return StringUtils.length(clickerRegistration.getClickerId()) <= CLICKERID_LENGTH;
    }

    /**
     * Get the instance.
     *
     * @return the instane
     */
    public static IClickerLogicImpl getInstance() {
        return IClickerLogicImpl.instance;
    }

    protected static void setInstance(IClickerLogicImpl instance) {
        IClickerLogicImpl.instance = instance;
    }

}
