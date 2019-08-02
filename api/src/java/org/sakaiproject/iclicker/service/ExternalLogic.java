package org.sakaiproject.iclicker.service;

import java.util.Locale;

public interface ExternalLogic {

    /**
     * Get the current user ID.
     *
     * @return the current user ID
     */
    String getCurrentUserId();

    /**
     * Get the current locale.
     *
     * @return the current locale
     */
    Locale getCurrentLocale();

    /**
     * Is the user an admin?
     *
     * @param userId the user ID
     * @return true, if is an admin
     */
    boolean isUserAdmin(String userId);

    /**
     * Is the user an instructor?
     *
     * @param userId the user ID
     * @return true, if is an instructor
     */
    boolean isUserInstructor(String userId);

    /**
     * Is this session valid?
     *
     * @param sessionId the session ID
     * @param defaultValue the default value
     * @return true, if session valid
     */
    boolean validateSessionId(String sessionId, boolean defaultValue);

}
