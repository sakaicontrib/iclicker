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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.RoleAlreadyDefinedException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.user.api.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test class for the Sakai Site object<br/>
 * This has to be here since I cannot create a Site object in Sakai for some reason... sure would be nice if I could though -AZ
 */
@SuppressWarnings("unchecked")
public class FakeSite implements Site {

    private static final long serialVersionUID = 4761288804996964705L;

    private String id;
    private String title = "Title";
    private String reference = "/site/default";

    public FakeSite() {
    }

    public FakeSite(String id, String title, String reference) {
        this.id = id;
        this.title = title;
        this.reference = reference;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#addGroup()
     */
    public Group addGroup() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#addPage()
     */
    public SitePage addPage() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getCreatedBy()
     */
    public User getCreatedBy() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getCreatedTime()
     */
    public Time getCreatedTime() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getDescription()
     */
    public String getDescription() {
        return "Description";
    }

    @Override
    public String getHtmlDescription() {
        return "<span>description</span>";
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getGroup(java.lang.String)
     */
    public Group getGroup(String id) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getGroups()
     */
    public Collection getGroups() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getGroupsWithMember(java.lang.String)
     */
    public Collection getGroupsWithMember(String userId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getGroupsWithMemberHasRole(java.lang.String, java.lang.String)
     */
    public Collection getGroupsWithMemberHasRole(String userId, String role) {
        return null;
    }

    public Collection<String> getMembersInGroups(Set<String> strings) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getIconUrl()
     */
    public String getIconUrl() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getIconUrlFull()
     */
    public String getIconUrlFull() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getInfoUrl()
     */
    public String getInfoUrl() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getInfoUrlFull()
     */
    public String getInfoUrlFull() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getJoinerRole()
     */
    public String getJoinerRole() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getModifiedBy()
     */
    public User getModifiedBy() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getModifiedTime()
     */
    public Time getModifiedTime() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getOrderedPages()
     */
    public List getOrderedPages() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getPage(java.lang.String)
     */
    public SitePage getPage(String id) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getPages()
     */
    public List getPages() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getShortDescription()
     */
    public String getShortDescription() {
        return "Short desc";
    }

    @Override
    public String getHtmlShortDescription() {
        return "<span>short desc</span>";
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getSkin()
     */
    public String getSkin() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getTitle()
     */
    public String getTitle() {
        return title;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getTool(java.lang.String)
     */
    public ToolConfiguration getTool(String id) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getToolForCommonId(java.lang.String)
     */
    public ToolConfiguration getToolForCommonId(String commonToolId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getTools(java.lang.String[])
     */
    public Collection getTools(String[] toolIds) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getTools(java.lang.String)
     */
    public Collection getTools(String commonToolId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#getType()
     */
    public String getType() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#hasGroups()
     */
    public boolean hasGroups() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#isJoinable()
     */
    public boolean isJoinable() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#isPubView()
     */
    public boolean isPubView() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#isPublished()
     */
    public boolean isPublished() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#isType(java.lang.Object)
     */
    public boolean isType(Object type) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#loadAll()
     */
    public void loadAll() {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#regenerateIds()
     */
    public void regenerateIds() {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#removeGroup(org.sakaiproject.site.api.Group)
     */
    public void removeGroup(Group group) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#removePage(org.sakaiproject.site.api.SitePage)
     */
    public void removePage(SitePage page) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setDescription(java.lang.String)
     */
    public void setDescription(String description) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setIconUrl(java.lang.String)
     */
    public void setIconUrl(String url) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setInfoUrl(java.lang.String)
     */
    public void setInfoUrl(String url) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setJoinable(boolean)
     */
    public void setJoinable(boolean joinable) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setJoinerRole(java.lang.String)
     */
    public void setJoinerRole(String role) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setPubView(boolean)
     */
    public void setPubView(boolean pubView) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setPublished(boolean)
     */
    public void setPublished(boolean published) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setShortDescription(java.lang.String)
     */
    public void setShortDescription(String description) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setSkin(java.lang.String)
     */
    public void setSkin(String skin) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setTitle(java.lang.String)
     */
    public void setTitle(String title) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.site.api.Site#setType(java.lang.String)
     */
    public void setType(String type) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Edit#getPropertiesEdit()
     */
    public ResourcePropertiesEdit getPropertiesEdit() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Edit#isActiveEdit()
     */
    public boolean isActiveEdit() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Entity#getId()
     */
    public String getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Entity#getProperties()
     */
    public ResourceProperties getProperties() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Entity#getReference()
     */
    public String getReference() {
        return reference;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Entity#getReference(java.lang.String)
     */
    public String getReference(String rootProperty) {
        return reference;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Entity#getUrl()
     */
    public String getUrl() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Entity#getUrl(java.lang.String)
     */
    public String getUrl(String rootProperty) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.entity.api.Entity#toXml(org.w3c.dom.Document, java.util.Stack)
     */
    public Element toXml(Document doc, Stack stack) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) {
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#addMember(java.lang.String, java.lang.String, boolean, boolean)
     */
    public void addMember(String userId, String roleId, boolean active, boolean provided) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#addRole(java.lang.String)
     */
    public Role addRole(String id) throws RoleAlreadyDefinedException {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#addRole(java.lang.String, org.sakaiproject.authz.api.Role)
     */
    public Role addRole(String id, Role other) throws RoleAlreadyDefinedException {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getMaintainRole()
     */
    public String getMaintainRole() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getMember(java.lang.String)
     */
    public Member getMember(String userId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getMembers()
     */
    public Set getMembers() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getProviderGroupId()
     */
    public String getProviderGroupId() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getRole(java.lang.String)
     */
    public Role getRole(String id) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getRoles()
     */
    public Set getRoles() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getRolesIsAllowed(java.lang.String)
     */
    public Set getRolesIsAllowed(String function) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getUserRole(java.lang.String)
     */
    public Role getUserRole(String userId) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getUsers()
     */
    public Set getUsers() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getUsersHasRole(java.lang.String)
     */
    public Set getUsersHasRole(String role) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#getUsersIsAllowed(java.lang.String)
     */
    public Set getUsersIsAllowed(String function) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#hasRole(java.lang.String, java.lang.String)
     */
    public boolean hasRole(String userId, String role) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#isAllowed(java.lang.String, java.lang.String)
     */
    public boolean isAllowed(String userId, String function) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#isEmpty()
     */
    public boolean isEmpty() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#keepIntersection(org.sakaiproject.authz.api.AuthzGroup)
     */
    public boolean keepIntersection(AuthzGroup other) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#removeMember(java.lang.String)
     */
    public void removeMember(String userId) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#removeMembers()
     */
    public void removeMembers() {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#removeRole(java.lang.String)
     */
    public void removeRole(String role) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#removeRoles()
     */
    public void removeRoles() {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#setMaintainRole(java.lang.String)
     */
    public void setMaintainRole(String role) {
    }

    /*
     * (non-Javadoc)
     * @see org.sakaiproject.authz.api.AuthzGroup#setProviderGroupId(java.lang.String)
     */
    public void setProviderGroupId(String id) {
    }

    public boolean isCustomPageOrdered() {
        return false;
    }

    public void setCustomPageOrdered(boolean custom) {
    }

    public boolean isSoftlyDeleted() {
        return false;
    }

    public Date getSoftlyDeletedDate() {
        return null;
    }

    public void setSoftlyDeleted(boolean b) {
    }

    public Date getCreatedDate() {
        return new Date();
    }

    public Date getModifiedDate() {
        return new Date();
    }

    @Override
    public Collection<Group> getGroupsWithMembers(String[] userIds) {
        return null;
    }

}
