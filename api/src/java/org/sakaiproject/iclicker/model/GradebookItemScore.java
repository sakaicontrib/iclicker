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
package org.sakaiproject.iclicker.model;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This represents an individual score in a gradebook grade item for a user.
 */
@Data
@NoArgsConstructor
public class GradebookItemScore {
    private String id;
    private String itemName;
    private String userId;
    private String username;
    private String graderUserId;
    private Date recorded;
    private String grade;
    private String comment;

    /**
     * Used to indicate the failure that occurred when saving this score (null if no error).
     */
    private String error;

    /**
     * Constructor.
     *
     * @param itemName the item name
     * @param userId the user ID
     * @param grade the grade
     */
    public GradebookItemScore(String itemName, String userId, String grade) {
        this(itemName, userId, grade, null, null, null, null);
    }

    /**
     * Constructor.
     *
     * @param itemName the item name
     * @param userId the user ID
     * @param grade the grade
     * @param username the username
     * @param graderUserId the grader ID
     * @param recorded the date of recording
     * @param comment the comment
     */
    public GradebookItemScore(
            String itemName, String userId, String grade, String username,
            String graderUserId, Date recorded, String comment) {
        if (StringUtils.isBlank(itemName)) {
            throw new IllegalArgumentException("itemName must be set");
        }
        if (StringUtils.isBlank(grade)) {
            throw new IllegalArgumentException("grade must be set");
        }
        if (StringUtils.isBlank(userId) && StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("userId or username must be set");
        }

        this.id = itemName + ":" + (userId != null ? userId : username);
        this.itemName = itemName;
        this.userId = userId;
        this.username = username;
        this.graderUserId = graderUserId;

        if (recorded == null) {
            this.recorded = new Date();
        } else {
            this.recorded = new Date(recorded.getTime());
        }

        this.grade = grade;
        this.comment = comment;
    }

    /**
     * Assign the ID.
     *
     * @param itemsName the item name
     * @param usersId the users ID
     */
    public void assignId(String itemsName, String usersId) {
        this.id = itemsName + ":" + usersId;
        this.itemName = itemsName;
        this.userId = usersId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((grade == null) ? 0 : grade.hashCode());
        result = prime * result + ((itemName == null) ? 0 : itemName.hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        GradebookItemScore other = (GradebookItemScore) obj;

        if (grade == null) {
            if (other.grade != null) {
                return false;
            }
        } else if (!StringUtils.equals(grade, other.grade)) {
            return false;
        }

        if (itemName == null) {
            if (other.itemName != null) {
                return false;
            }
        } else if (!StringUtils.equals(itemName, other.itemName)) {
            return false;
        }

        if (userId == null) {
            if (other.userId != null) {
                return false;
            }
        } else if (!StringUtils.equals(userId, other.userId)) {
            return false;
        }

        return true;
    }

}
