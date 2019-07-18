/**
 * Copyright (c) 2003 i>clicker (R) <http://www.iclicker.com/dnn/>
 *
 * This file is part of Sakai iclicker project base pom.
 *
 * Sakai iclicker project base pom is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sakai iclicker project base pom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sakai iclicker project base pom.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sakaiproject.iclicker.model;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a user in the system
 * 
 */
@Data
@NoArgsConstructor
public class User {

    private String userId;
    private String username;
    private String name;
    private String fname;
    private String lname;
    private String email;
    private String sortName;

    public User(String userId, String username, String name) {
        this(userId, username, name, null, null);
    }

    public User(String userId, String username, String name, String sortName, String email) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.sortName = sortName;
        this.email = email;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());

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

        User other = (User) obj;

        if (userId == null) {
            if (other.userId != null) {
                return false;
            }
        } else if (!StringUtils.equals(userId, other.userId)) {
            return false;
        }

        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!StringUtils.equals(username, other.username)) {
            return false;
        }

        return true;
    }

    public static class UsernameComparator implements Comparator<User>, Serializable {

        public static final long serialVersionUID = 1L;

        public int compare(User o1, User o2) {
            return o1.username.compareTo(o2.username);
        }

    }

    public static class SortnameComparator implements Comparator<User>, Serializable {

        public static final long serialVersionUID = 1L;

        public int compare(User o1, User o2) {
            return o1.sortName.compareTo(o2.sortName);
        }

    }

}
