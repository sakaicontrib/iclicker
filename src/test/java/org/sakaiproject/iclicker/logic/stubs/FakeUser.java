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

import java.util.Date;
import java.util.Stack;

import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.user.api.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test class for the Sakai User object<br/>
 * This has to be here since I cannot create a User object in Sakai for some reason... sure would be nice if I could though -AZ
 */
@SuppressWarnings("unchecked")
public class FakeUser implements User {
    private String userId;
    private String userEid = "fakeEid";
    private String displayName = "Fake DisplayName";

    public FakeUser() {
    }

    /**
     * Construct an empty test user with an id set
     * 
     * @param userId a id string
     */
    public FakeUser(String userId) {
        this.userId = userId;
    }

    /**
     * Construct an empty test user with an id and eid set
     * 
     * @param userId a id string
     * @param userEid a username string
     */
    public FakeUser(String userId, String userEid) {
        this.userId = userId;
        this.userEid = userEid;
    }

    /**
     * Construct an empty test user with an id and eid set
     * 
     * @param userId a id string
     * @param userEid a username string
     * @param displayName a user display name
     */
    public FakeUser(String userId, String userEid, String displayName) {
        this.userId = userId;
        this.userEid = userEid;
        this.displayName = displayName;
    }

    public boolean checkPassword(String pw) {
        return false;
    }

    public User getCreatedBy() {
        return null;
    }

    public String getDisplayId() {
        return null;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getEid() {
        return this.userEid;
    }

    public String getEmail() {
        return null;
    }

    public String getFirstName() {
        return null;
    }

    public String getLastName() {
        return null;
    }

    public User getModifiedBy() {
        return null;
    }

    public String getSortName() {
        return null;
    }

    public String getType() {
        return null;
    }

    public String getId() {
        return userId;
    }

    public ResourceProperties getProperties() {
        return null;
    }

    public String getReference() {
        return null;
    }

    public String getReference(String rootProperty) {
        return null;
    }

    public String getUrl() {
        return null;
    }

    public String getUrl(String rootProperty) {
        return null;
    }

    public Element toXml(Document doc, Stack stack) {
        return null;
    }

    public int compareTo(Object arg0) {
        return 0;
    }

    public Time getCreatedTime() {
        return null;
    }

    public Time getModifiedTime() {
        return null;
    }

    public Date getCreatedDate() {
        return new Date();
    }

    public Date getModifiedDate() {
        return new Date();
    }

}
