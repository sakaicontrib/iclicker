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

import org.apache.commons.lang.StringUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * This represents an individual score in a gradebook grade item for a user
 */
public class GradebookItemScore {
    @Setter @Getter private String id;
    @Setter @Getter private String itemName;
    @Setter @Getter private String userId;
    @Setter @Getter private String username;
    @Setter @Getter private String graderUserId;
    @Setter @Getter private Date recorded;
    @Setter @Getter private String grade;
    @Setter @Getter private String comment;

    /**
     * Used to indicate the failure that occurred when saving this score (null if no error)
     */
    @Setter @Getter private String error;

    protected GradebookItemScore() {
    }

    public GradebookItemScore(String itemName, String userId, String grade) {
        this(itemName, userId, grade, null, null, null, null);
    }

    public GradebookItemScore(String itemName, String userId, String grade, String username, String graderUserId, Date recorded, String comment) {
        if (StringUtils.isBlank(itemName)) {
            throw new IllegalArgumentException("itemName must be set");
        }
        if (StringUtils.isBlank(grade)) {
            throw new IllegalArgumentException("grade must be set");
        }
        if (StringUtils.isBlank(userId) && StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("userId or username must be set");
        }

        assignId(itemName, userId != null ? userId : username);
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

    public void assignId(String itemName, String userId) {
        this.id = itemName + ":" + userId;
        this.itemName = itemName;
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "(" + id + ":" + username + ":" + grade + ":" + recorded + ")";
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
