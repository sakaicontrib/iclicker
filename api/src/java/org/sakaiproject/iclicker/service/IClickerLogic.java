package org.sakaiproject.iclicker.service;

import org.sakaiproject.iclicker.model.Course;
import org.sakaiproject.iclicker.model.Gradebook;
import org.sakaiproject.iclicker.model.GradebookItem;
import org.sakaiproject.iclicker.model.Student;
import org.sakaiproject.iclicker.model.dao.ClickerRegistration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface IClickerLogic {

    /**
     * Init.
     */
    void init();

    /**
     * @return true if SSO handling is enabled, false otherwise
     */
    boolean isSingleSignOnEnabled();

    /**
     * Sends a notification to the list of admins, this is primarily for
     * notifications of failures related to webservices failures.
     * 
     * @param message the notification message to send
     * @param failure [OPTIONAL] the exception if there was one
     */
    void sendNotification(String message, Exception failure);

    /**
     * resolve the i18n message.
     * 
     * @param key the message key to lookup
     * @param locale the Locale in which to do the lookup
     * @param args Array of arguments that will be filled in for params within
     *        the message (params look like "{0}", "{1,date}", "{2,time}" within a message), or null if none
     * @return the i18n string OR null if the key cannot be resolved
     */
    String getMessage(String key, Locale locale, Object... args);

    // *******************************************************************************
    // SSO handling

    /**
     * Attempt to authenticate a user given a login name and password (or user passkey).
     * 
     * @param loginname the login name for the user
     * @param password the password for the user (might be the SSO user passkey)
     * @param ssoKey [OPTIONAL] the SSO encoded key if one exists in the request
     * @return the session ID if authenticated OR null if the auth parameters are invalid
     */
    String authenticate(String loginname, String password, String ssoKey);

    /**
     * Make or find a user key for the given user, if they don't have one,
     * this will create one, if they do it will retrieve it. It can also be used to generate a new user key.
     * 
     * @param userId [OPTIONAL] the internal user id (if null, use the current user id)
     * @param makeNew if true, make a new key even if the user already has one,
     *        if false, only make a key if they do not have one
     * @return the user key for the given user
     * @throws IllegalStateException if user is not set or is not an instructor
     */
    String makeUserKey(String userId, boolean makeNew);

    /**
     * Checks if the passed in user key is valid compared to the internally stored user key.
     * 
     * @param userId [OPTIONAL] the internal user id (if null, use the current user id)
     * @param userKey the passed in SSO key to check for this user
     * @return true if the key is valid OR false if the user has no key or the key is otherwise invalid
     */
    boolean checkUserKey(String userId, String userKey);

    /**
     * @param sharedKey set and verify the shared key (if invalid, log a warning)
     */
    void setSharedKey(String sharedKey);

    /**
     * @return the SSO shared key value (or empty string otherwise)
     * NOTE: this only works if SSO is enabled AND the user is an admin.
     */
    String getSharedKey();

    /**
     * Verify the passed in encrypted SSO shared key is valid,
     * this will return false if the key is not configured Key must have been encoded
     * like so (where timestamp is the unix time in seconds):
     * sentKey = hex(sha1(sharedKey + ":" + timestamp)) + "|" + timestamp.
     * 
     * @param key the passed in key (should already be sha-1 and hex encoded with the timestamp appended)
     * @return true if the key is valid, false if SSO shared keys are disabled
     * @throws IllegalArgumentException if the key format is invalid
     * @throws SecurityException if the key timestamp has expired or the key does not match
     */
    boolean verifyKey(String key);

    // *******************************************************************************
    // Admin i>clicker tool in workspace handling

    /**
     * Executes the add or remove tool workspace operation.
     * 
     * @param type the type of runner to make, from BigRunner.RUNNER_TYPE_*
     * @return the runner object indicating progress
     * @throws IllegalArgumentException if the runner type is unknown
     * @throws IllegalStateException if there is a runner in progress of a different type
     * @throws ClickerLockException if a lock cannot be obtained
     */
    BigRunner startRunnerOperation(String type);

    /**
     * Get the currently running process if there is one.
     * 
     * @return the runner OR null if there is no running process
     */
    BigRunner getRunnerStatus();

    /**
     * clears the holder.
     */
    void clearRunner();

    // *******************************************************************************
    // Clicker registration handling

    /**
     * This returns an item based on an id if the user is allowed to access it.
     * 
     * @param id the id of the item to fetch
     * @return a ClickerRegistration or null if none found
     * @throws SecurityException if the current user cannot access this item
     */
    ClickerRegistration getItemById(Long id);

    /**
     * This returns an item based on a clickerId for the current user if allowed to access it,
     * this will return a null if the clickerId happens to be invalid or cannot be found.
     * 
     * @param clickerId the clicker remote ID
     * @return a ClickerRegistration OR null if none found
     * @throws SecurityException if the current user cannot access this item
     */
    ClickerRegistration getItemByClickerId(String clickerId);

    /**
     * This returns an item based on a clickerId and ownerId if the user is allowed to access it,
     * this will return a null if the clickerId is invalid or cannot be found.
     * 
     * @param clickerId the clicker remote ID
     * @param ownerId the clicker owner ID (user id)
     * @return a ClickerRegistration OR null if none found
     * @throws SecurityException if the current user cannot access this item
     */
    ClickerRegistration getItemByClickerId(String clickerId, String ownerId);

    /**
     * @param item registration
     * @param userId sakai user id
     * @return true if the item can be read, false otherwise
     */
    boolean canReadItem(ClickerRegistration item, String userId);

    /**
     * Check if a specified user can write this item in a specified site.
     * 
     * @param item to be modified or removed
     * @param userId the internal user id (not username)
     * @return true if item can be modified, false otherwise
     */
    boolean canWriteItem(ClickerRegistration item, String userId);

    /**
     * This returns a List of items that are visible for a specified user.
     * 
     * @param userId the internal user id (not username)
     * @param locationId [OPTIONAL] a unique id which represents the current location of the user (entity reference)
     * @return a List of ClickerRegistration objects
     */
    List<ClickerRegistration> getAllVisibleItems(String userId, String locationId);

    /**
     * ADMIN ONLY Only the admin can use this method to retrieve all clicker IDs.
     * 
     * @param first the first item (for paging and limiting)
     * @param max the max number of items to return
     * @param order [OPTIONAL] sort order for the items
     * @param searchStr [OPTIONAL] search by partial clickerId
     * @param includeUserDisplayNames if true the user display names are added to the results
     * @return a list of clicker registrations
     */
    List<ClickerRegistration> getAllItems(
            int first, int max, String order, String searchStr, boolean includeUserDisplayNames);

    /**
     * Count all items.
     *
     * @return the count
     */
    int countAllItems();

    /**
     * Get user display name.
     *
     * @param userId the user ID
     * @return the user display name
     */
    String getUserDisplayName(String userId);

    /**
     * Remove an item NOTE: only admins can fully remove a registration.
     * 
     * @param item the ClickerRegistration to remove
     * @throws SecurityException if the user not allowed to remove the registration
     */
    void removeItem(ClickerRegistration item);

    /**
     * Save (Create or Update) an item (uses the current site).
     * 
     * @param item the ClickerRegistration to create or update
     * @throws IllegalArgumentException if the item is null OR the owner id is not a valid user
     * @throws SecurityException if the user cannot save the registration for lack of perms
     * @throws ClickerIdInvalidException if the clicker ID is invalid for some reason,
     *         the exception will indicate the type of validation failure
     */
    void saveItem(ClickerRegistration item);

    /**
     * Creates a new clicker remote registration in the system for the current user, also push to national.
     * 
     * @param clickerId the clicker remote ID
     * @return the registration
     * @throws ClickerIdInvalidException if the clicker ID is invalid for some reason,
     *         the exception will indicate the type of validation failure
     * @throws ClickerRegisteredException if the clickerId is already registered
     * @throws SecurityException if the user cannot save the registration for lacks of perms
     */
    ClickerRegistration createRegistration(String clickerId);

    /**
     * Creates a new clicker remote registration in the system, will push the registration to national as well.
     * 
     * @param clickerId the clicker remote ID
     * @param ownerId the owner of this registration
     * @return the registration
     * @throws ClickerIdInvalidException if the clicker ID is invalid for some reason,
     *         the exception will indicate the type of validation failure
     * @throws ClickerRegisteredException if the clickerId is already registered
     * @throws IllegalArgumentException if the owner id is not a valid user
     * @throws SecurityException if the user cannot save the registration for lacks of perms
     */
    ClickerRegistration createRegistration(String clickerId, String ownerId);

    /**
     * Updates the activation of a registration for the current user.
     * 
     * @param registrationId the unique id of the registration
     * @param activated the new activation level
     * @return the registration if it was updated, null if not updated
     * @throws IllegalArgumentException if the registrationId is invalid
     * @throws SecurityException is the current user cannot update the registration
     */
    ClickerRegistration setRegistrationActive(Long registrationId, boolean activated);

    /**
     * Gets students for the course with a clicker registered.
     *
     * @param courseId the course ID
     * @return the list of students with a clicker registered
     */
    List<Student> getStudentsForCourseWithClickerReg(String courseId);

    /**
     * Get the clicker registrations for a given user.
     * 
     * @param userId the id of the user
     * @param activeOnly if true only get the active registrations, else get all
     * @return the list of registrations for this user
     */
    List<ClickerRegistration> getClickerRegistrationsForUser(String userId, boolean activeOnly);

    /**
     * Get all the courses for the given user, note that this needs to be limited from outside
     * this method for security, if the return is limited to a single course then the students are included.
     * 
     * @param courseId [OPTIONAL] limit the return to just this one site
     * @return the courses (up to 100 of them) which the user has instructor access in
     */
    List<Course> getCoursesForInstructorWithStudents(String courseId);

    /**
     * @param courseId a unique id which represents the current location of the user (entity reference)
     * @return the title for the context or "--------" (8 hyphens) if none found
     */
    String getCourseTitle(String courseId);

    /**
     * Gets the gradebook data for a given site, this uses the gradebook security so it is safe for anyone to call.
     * 
     * @param courseId a sakai siteId (cannot be group Id)
     * @param gbItemName [OPTIONAL] an item name to fetch from this gradebook (limit to this item only),
     * if null then all items are returned
     * @return the gradebook
     */
    Gradebook getCourseGradebook(String courseId, String gbItemName);

    /**
     * Save a gradebook item and optionally the scores within <br/>
     * Scores must have at least the studentId or username AND the grade set.
     * 
     * @param gbItem the gradebook item to save, must have at least the gradebookId and name set
     * @return the updated gradebook item and scores, contains any errors that occurred
     */
    GradebookItem saveGradebookItem(GradebookItem gbItem);

    /**
     * Save a gradebook (saves all items in the gradebook).
     * 
     * @param gb the gradebook to save
     * @return the updated gradebook items and scores, contains any errors that occurred
     */
    List<GradebookItem> saveGradebook(Gradebook gb);

    // **********************************************************************************
    // DATA encoding methods - put the data into the format desired by iclicker

    /**
     * Encode clicker registration.
     *
     * @param registration the registration
     * @return the encoded registration
     */
    String encodeClickerRegistration(ClickerRegistration registration);

    /**
     * Encode response from registration of clicker data This option should be available where the
     * instructor already has the clicker registration file (Remoteid.csv) and wants to upload the
     * registration(s) to the CMS Server.
     * 
     * @param registrations the registrations resulting from the action
     * @param status true if new registration, false otherwise
     * @param message the human readable message
     * @return the encoded XML
     * @throws IllegalStateException if the user cannot be found
     * @throws IllegalArgumentException if the data is invalid
     */
    String encodeClickerRegistrationResult(List<ClickerRegistration> registrations, boolean status, String message);

    /**
     * The xml format to upload students registration to the CMS Server remains the same as the national
     * registration web services and this upload should be treated as if the user is registering the
     * remotes manually inside the plug-in i.e all applicable messages should be returned <br/>
     * NOTE: we are ignoring the email and name inputs because we will get them from the user lookup of the id.
     * 
     * @param xml XML
     * @return the clicker registration object from the xml
     * @throws IllegalArgumentException if the xml is invalid or blank
     * @throws RuntimeException if there is an internal failure in the XML parser
     */
    ClickerRegistration decodeClickerRegistration(String xml);

    /**
     * Encode courses.
     *
     * @param instructorId the instructor ID
     * @param courses the courses
     * @return the encoded string
     */
    String encodeCourses(String instructorId, List<Course> courses);

    /**
     * Encode gradebook.
     *
     * @param gradebook the gradebook
     * @return the encoded string
     */
    String encodeGradebook(Gradebook gradebook);

    /**
     * Encode enrollments.
     *
     * @param course the course
     * @return the encoded string
     */
    String encodeEnrollments(Course course);

    /**
     * Decode gradebook XML.
     *
     * @param xml the encoded XML
     * @return the decoded XML
     */
    Gradebook decodeGradebookXML(String xml);

    /**
     * Encode save gradebook results.
     *
     * @param courseId the course ID
     * @param items the items
     * @return the encoded string
     */
    String encodeSaveGradebookResults(String courseId, List<GradebookItem> items);

    /**
     * Makes the clicker IDs and dates.
     *
     * @param regs the clicker registrations
     * @return the string
     */
    String[] makeClickerIdsAndDates(Collection<ClickerRegistration> regs);

    /*
     * ************************************************************************
     * Clicker ID validation
     * ************************************************************************
     */

    /**
     * Cleans up and validates a given clickerId.
     * 
     * @param clickerId a remote clicker ID
     * @return the cleaned up and valid clicker ID
     * @throws ClickerIdInvalidException if the id is invalid for some reason,
     *         the exception will indicate the type of validation failure
     */
    String validateClickerId(String clickerId);

    /**
     * Cleans up and validates a given clickerId.
     * 
     * @param clickerId a remote clicker ID
     * @param lastName OPTIONAL user lastname (only used for GO checks), defaults to current user
     * @return the cleaned up and valid clicker ID
     * @throws ClickerIdInvalidException if the id is invalid for some reason,
     *         the exception will indicate the type of validation failure
     */
    String validateClickerId(String clickerId, String lastName);

    /**
     * For all remoteids starting with 2, 4, 8, we have to generate an alternate id and concatenate
     * it with the existing remote ids for that particular user in the data sent to the iclicker desktop.
     * app (this is like creating an extra clickerid based on the existing ones)
     * 
     * @param clickerId a remote clicker ID
     * @return a translated clicker ID OR null if no translation is required or id is invalid
     */
    String translateClickerId(String clickerId);

    /**
     * Attempt to decode the XML into readable values in a map.
     *
     * @param xml XML
     * @return the map of attributes from S student record
     * @throws IllegalArgumentException if the xml is invalid or blank
     * @throws RuntimeException if there is an internal failure in the XML parser
     */
    Map<String, String> decodeGetRegisteredForClickerMACResult(String xml);

    /**
     * Get the domain URL.
     *
     * @return the domain URL
     */
    String getDomainURL();

    /**
     * Get the workspace page title.
     *
     * @return the workspace page title
     */
    String getWorkspacePageTitle();

    /**
     * Get failures.
     *
     * @return the list of failures
     */
    List<String> getFailures();

    /**
     * Is debugging on?
     *
     * @return true, if enabled
     */
    boolean isForceRestDebugging();

    /**
     * Get the external logic.
     *
     * @return the external logic
     */
    ExternalLogic getExternalLogic();

}
