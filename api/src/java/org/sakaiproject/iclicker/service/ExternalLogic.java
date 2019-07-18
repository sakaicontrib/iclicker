package org.sakaiproject.iclicker.service;

import java.util.Locale;

public interface ExternalLogic {

    String getCurrentUserId();
    Locale getCurrentLocale();
    boolean isUserAdmin(String userId);
    boolean isUserInstructor(String userId);
    boolean validateSessionId(String sessionId, boolean defaultValue);

}
