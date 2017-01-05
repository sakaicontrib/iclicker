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
<% controller.processInstructorSSO(pageContext, request); %>
<ul class="navIntraTool actionToolBar nav_items">
    <li class="firstToolBarItem nav_item"><a href="${regPath}"><fmt:message key="reg.title" /></a></li>
    <li class="nav_item"><a href="${instPath}"><fmt:message key="inst.title" /></a></li>
    <li class="nav_item"><span class="current"><fmt:message key="inst.sso.link" /></span></li>
</ul>

<h3 class="insColor insBak insBorder page_header">&nbsp;<fmt:message key="inst.sso.title">Sample Title</fmt:message></h3>
<!-- show messages if there are any to show -->
<jsp:include page="/views/userMsgs.jsp"></jsp:include>

<c:choose>
<c:when test="${ssoEnabled}">
<div class="inst_sso_instructions">
    <fmt:message key="inst.sso.instructions">SSO instructor instructions</fmt:message>
</div>
<div class="inst_sso_controls">
    <span class="sso_control_message"><fmt:message key="inst.sso.key.message">Your SSO passkey is</fmt:message>: </span>
    <span class="sso_control_key">${ssoUserKey}</span>
    <form class="sso_control_form" method="post" style="display:inline;">
        <input type="submit" class="generate_button" name="generateKey" value="<fmt:message key="inst.sso.generate.key">Generate a new key</fmt:message>" />
    </form>
</div>
</c:when>
<c:otherwise>
<div class="error"><fmt:message key="inst.sso.disabled">SSO is not enabled, you cannot access or generate a passkey</fmt:message></div>
</c:otherwise>
</c:choose>
