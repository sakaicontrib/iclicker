<%--

    Copyright (c) 2009 i>clicker (R) <http://www.iclicker.com/dnn/>

    This file is part of i>clicker Sakai integrate.

    i>clicker Sakai integrate is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    i>clicker Sakai integrate is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with i>clicker Sakai integrate.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%@ include file="/views/include.jsp" %>
<% controller.processAdmin(pageContext, request); %>
<div class="iclicker">

<ul class="navIntraTool actionToolBar nav_items">
    <li class="firstToolBarItem nav_item"><a href="${regPath}"><fmt:message key="reg.title" /></a></li>
    <li class="nav_item"><span class="current"><fmt:message key="admin.title" /></span></li>
</ul>

<h3 class="insColor insBak insBorder page_header">&nbsp;<fmt:message key="app.iclicker">iClicker</fmt:message> <fmt:message key="admin.title">Sample Title</fmt:message></h3>
<!-- show messages if there are any to show -->
<jsp:include page="/views/userMsgs.jsp"></jsp:include>

<div class="admin_controls">
    <c:if test="${runnerExists}">
    <div class="process_status">
        <fmt:message key="admin.process.header">Running process status:</fmt:message>
        <span class="runner_type"><fmt:message key="admin.process.type.${runnerType}">Adding</fmt:message></span> :
        <span id="runnerStatus" class="runner_status">${runnerPercent}%</span>
    </div>
    </c:if>
    <div class="workspace_form">
        <form method="post" style="display:inline;">
            <input type="hidden" name="runner" value="runner" />
            <input type="submit" class="runner_button" name="addAll" value="<fmt:message key="admin.process.add">Add ALL</fmt:message>" />
            <input type="submit" class="runner_button" name="removeAll" value="<fmt:message key="admin.process.remove">Remove ALL</fmt:message>" />
        </form>
    </div>
    <c:if test="${runnerExists}">
    <script type="text/javascript">Iclicker.initStatusChecker("#runnerStatus");</script>
    </c:if>
</div>

<c:if test="${fn:length(recentFailures) > 0}">
<div class="admin_errors">
    <fieldset class="visibleFS">
        <legend class="admin_errors_header">
            <fmt:message key="admin.errors.header">Errors</fmt:message>
        </legend>
        <ul class="tight admin_errors_list">
            <c:forEach var="message" items="${recentFailures}">
            <li class="admin_errors_list_item">${message}</li>
            </c:forEach>
        </ul>
    </fieldset>
</div>
</c:if>

<div class="main_content">
    <!-- pager control -->
    <div class="paging_bar"><fmt:message key="admin.paging">Paging:</fmt:message>
        <c:choose><c:when test="${totalCount > 0}">${pagerHTML}</c:when>
        <c:otherwise><i><fmt:message key="admin.no.regs">No registrations</fmt:message></i></c:otherwise></c:choose>
    </div>

    <!-- clicker registration listing -->
    <table width="90%" border="1" cellspacing="0" cellpadding="0" 
        summary="<fmt:message key="admin.regs.table.summary" />">
        <thead>
            <tr class="registration_row header_row">
                <th width="30%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5">
                    <fmt:message key="admin.username.header">User name</fmt:message>
                </th>
                <th width="20%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5">
                    <a href="${adminPath}&sort=clickerId&page=${page}"><fmt:message key="reg.remote.id.header">clicker Remote ID</fmt:message></a>
                </th>
                <th width="20%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5">
                    <a href="${adminPath}&sort=dateCreated&page=${page}"><fmt:message key="reg.registered.date.header">Registered</fmt:message></a>
                </th>
                <th width="30%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5" nowrap="nowrap">
                    <a href="${adminPath}&sort=activated&page=${page}"><fmt:message key="admin.controls.header">Controls</fmt:message></a>
                </th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="registration" items="${registrations}">
            <tr class="registration_row data_row style1 ${registration.activated ? '' : 'disabled'}">
                <td class="user_name" align="center">${registration.userDisplayName}</td>
                <td class="clicker_id" align="center">${registration.clickerId}</td>
                <td class="date" align="center"><fmt:formatDate value="${registration.dateCreated}" pattern="yyyy/MM/dd"/></td>
                <td class="controls" align="center">
                    <form method="post">
                        <input type="hidden" name="view" value="${adminView}" />
                        <input type="hidden" name="page" value="${page}" />
                        <input type="hidden" name="sort" value="${sort}" />
                        <input type="hidden" name="registrationId" value="${registration.id}" />
                    <c:choose><c:when test="${registration.activated}">
                        <input type="button" class="small" value="<fmt:message key="app.activate">Activate</fmt:message>" disabled="disabled" />
                        <input type="submit" class="small" value="<fmt:message key="app.disable">Disable</fmt:message>" alt="<fmt:message key="reg.disable.submit.alt" />" />
                        <input type="hidden" name="activate" value="false" />
                    </c:when><c:otherwise>
                        <input type="submit" class="small" value="<fmt:message key="app.activate">Activate</fmt:message>" alt="<fmt:message key="reg.reactivate.submit.alt" />" />
                        <input type="button" class="small" value="<fmt:message key="app.disable">Disable</fmt:message>" disabled="disabled" />
                        <input type="hidden" name="activate" value="true" />
                    </c:otherwise></c:choose>
                    </form>
                    <form method="post">
                        <input type="hidden" name="view" value="${adminView}" />
                        <input type="hidden" name="page" value="${page}" />
                        <input type="hidden" name="sort" value="${sort}" />
                        <input type="hidden" name="registrationId" value="${registration.id}" />
                        <input type="hidden" name="remove" value="false" />
                        <input type="submit" class="small" value="<fmt:message key="app.remove">Remove</fmt:message>" alt="<fmt:message key="admin.remove.submit.alt" />" />
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<div class="admin_config">
    <fieldset class="visibleFS">
        <legend class="admin_config_header">
            <fmt:message key="admin.config.header">Config</fmt:message>
        </legend>
        <ul class="tight admin_config_list">
            <c:if test="${ssoEnabled}">
            <li class="admin_config_list_item">
                <span class="sso_enabled"><fmt:message key="admin.config.ssoenabled">SSO Enabled</fmt:message></span>: 
                <fmt:message key="admin.config.ssosharedkey">Shared Key</fmt:message>: <span class="sso_shared_key">${ssoSharedKey}</span>
            </li>
            </c:if>
            <li class="admin_config_list_item"><fmt:message key="admin.config.domainurl">url</fmt:message>: ${domainURL}</li>
            <li class="admin_config_list_item"><fmt:message key="admin.config.workspacepagetitle">workspacePageTitle</fmt:message>: ${workspacePageTitle}</li>
        </ul>
    </fieldset>
</div>

</div>