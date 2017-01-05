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
package org.sakaiproject.iclicker.logic.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.sakaiproject.iclicker.impl.logic.FakeDataPreload;
import org.sakaiproject.iclicker.logic.ExternalLogic;
import org.sakaiproject.iclicker.model.Course;
import org.sakaiproject.iclicker.model.Student;
import org.sakaiproject.iclicker.model.User;

import lombok.Getter;

/**
 * Stub class for the external logic impl (for testing)
 */
public class ExternalLogicStub extends ExternalLogic {

    /**
     * represents the current user userId, can be changed to simulate multiple users
     */
    @Getter(onMethod = @__(@Override))
    public String currentUserId;

    /**
     * represents the current location, can be changed to simulate multiple locations
     */
    public String currentLocationId;

    /**
     * Reset the current user and location to defaults
     */
    public void setDefaults() {
        currentUserId = FakeDataPreload.USER_ID;
        currentLocationId = FakeDataPreload.LOCATION1_ID;
    }

    public ExternalLogicStub() {
        setDefaults();
    }

    @Override
    public String getCurrentLocationId() {
        return currentLocationId;
    }

    @Override
    public String getLocationTitle(String locationId) {
        if (StringUtils.equals(locationId, FakeDataPreload.LOCATION1_ID)) {
            return FakeDataPreload.LOCATION1_TITLE;
        } else if (StringUtils.equals(locationId, FakeDataPreload.LOCATION2_ID)) {
            return FakeDataPreload.LOCATION2_TITLE;
        }

        return "--------";
    }

    @Override
    public Locale getCurrentLocale() {
        return Locale.getDefault();
    }

    @Override
    public String getUserDisplayName(String userId) {
        if (StringUtils.equals(userId, FakeDataPreload.USER_ID)) {
            return FakeDataPreload.USER_DISPLAY;
        } else if (StringUtils.equals(userId, FakeDataPreload.ACCESS_USER_ID)) {
            return FakeDataPreload.ACCESS_USER_DISPLAY;
        } else if (StringUtils.equals(userId, FakeDataPreload.MAINT_USER_ID)) {
            return FakeDataPreload.MAINT_USER_DISPLAY;
        } else if (StringUtils.equals(userId, FakeDataPreload.ADMIN_USER_ID)) {
            return FakeDataPreload.ADMIN_USER_DISPLAY;
        }

        return "----------";
    }

    @Override
    public boolean isUserAdmin(String userId) {
        if (StringUtils.equals(userId, FakeDataPreload.ADMIN_USER_ID)) {
            return true;
        }

        return false;
    }

    @Override
    public String getNotificationEmail() {
        return "admin@sakai.com";
    }

    @Override
    public void sendEmails(String fromEmail, String[] toEmails, String subject, String body) {
        if (toEmails == null || toEmails.length == 0) {
            throw new IllegalArgumentException("invalid toEmails");
        }
    }

    @Override
    public boolean isUserAllowedInLocation(String userId, String permission, String locationId) {
        if (StringUtils.equals(userId, FakeDataPreload.USER_ID)) {
            if (StringUtils.equals(locationId, FakeDataPreload.LOCATION1_ID)) {
                return false;
            }
        } else if (StringUtils.equals(userId, FakeDataPreload.ACCESS_USER_ID)) {
            if (StringUtils.equals(locationId, FakeDataPreload.LOCATION1_ID)) {
                return false;
            }
        } else if (StringUtils.equals(userId, FakeDataPreload.MAINT_USER_ID)) {
            if (locationId.equals(FakeDataPreload.LOCATION1_ID)) {
                return true;
            }
        } else if (StringUtils.equals(userId, FakeDataPreload.ADMIN_USER_ID)) {
            // admin can do anything in any context
            return true;
        }

        return false;
    }

    @Override
    public boolean isUserInstructor(String userId) {
        if (StringUtils.equals(userId, FakeDataPreload.MAINT_USER_ID)) {
            return true;
        } else if (StringUtils.equals(userId, FakeDataPreload.ADMIN_USER_ID)) {
            // admin can do anything in any context but is not an instructor
            return false;
        }

        return false;
    }

    @Override
    public String isInstructorOfUser(String studentUserId) {
        if (StringUtils.equals(currentUserId, FakeDataPreload.MAINT_USER_ID)) {
            return FakeDataPreload.LOCATION1_ID;
        } else {
            return null;
        }
    }

    @Override
    public List<Course> getCoursesForInstructor(String siteId, int max) {
        List<Course> sites = new ArrayList<>();
        if (StringUtils.equals(currentUserId, FakeDataPreload.MAINT_USER_ID)) {
            if (StringUtils.equals(FakeDataPreload.LOCATION1_ID, siteId)) {
                Course c = new Course(FakeDataPreload.LOCATION1_ID, FakeDataPreload.LOCATION1_TITLE);

                if (siteId != null) {
                    c.setStudents(new ArrayList<Student>()); // fill with data?
                }

                sites.add(c);
            }
        }

        return sites;
    }

    @Override
    public <T> T getConfigurationSetting(String settingName, T defaultValue) {
        T returnValue = (T) defaultValue;

        return returnValue;
    };

    @Override
    public User getUser(String userId) {
        User user = new org.sakaiproject.iclicker.model.User(userId, "eid-" + userId, "DisplayName-" + userId, userId + "-sortname", userId + "@email.com");
        user.setFname("First");
        user.setLname("Last" + userId);

        return user;
    }

}
