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
<% controller.processInstructor(pageContext, request); %>
<ul class="navIntraTool actionToolBar nav_items">
    <li class="firstToolBarItem nav_item"><a href="${regPath}"><fmt:message key="reg.title" /></a></li>
    <li class="nav_item">
        <span class="current"><fmt:message key="inst.title" /></span> &gt;
        <c:choose><c:when test="${courseId == null}"><fmt:message key="inst.all.courses" /></c:when>
        <c:otherwise><a href="${instPath}"><fmt:message key="inst.all.courses" /></a> &gt; ${courseTitle}</c:otherwise></c:choose>
    </li>
    <c:if test="${ssoEnabled}">
    <li class="nav_item"><a href="${instSSOPath}"><fmt:message key="inst.sso.link" /></a></li>
    </c:if>
</ul>

<h3 class="insColor insBak insBorder page_header">&nbsp;<fmt:message key="app.iclicker">iClicker</fmt:message> <fmt:message key="inst.title">Sample Title</fmt:message></h3>
<!-- show messages if there are any to show -->
<jsp:include page="/views/userMsgs.jsp"></jsp:include>

<%-- commented out SSO message -AZ
<c:if test="${ssoEnabled}">
<fieldset class="visibleFS">
    <legend class="admin_config_header">
        <fmt:message key="admin.config.ssoenabled">SSO Enabled</fmt:message>
    </legend>
    <div class="inst_sso_controls">
        <fmt:message key="inst.sso.message">Single Sign On is enabled for this Sakai installation. You will need to use a passkey with i>clicker.</fmt:message>
        <a href="${instSSOPath}"><fmt:message key="inst.sso.link" /></a>
    </div>
</fieldset>
</c:if>
--%>

<div class="main_content">
<c:choose><c:when test="${coursesCount <= 0}">
    <span class="no_items"><fmt:message key="inst.no.courses">No courses</fmt:message></span>
</c:when><c:when test="${showStudents}">
    <div class="title"><fmt:message key="inst.course">Course</fmt:message>: ${course.title}</div>
    <div class="description">${course.description}</div>
    <!-- clicker registration listing -->
    <div><fmt:message key="inst.students">Students</fmt:message> (${studentsCount}):</div>
    <table width="80%" border="1" cellspacing="0" cellpadding="0" class="students_list"
        summary="<fmt:message key="inst.students.table.summary" />">
        <thead>
            <tr class="students_header header_row">
                <th width="40%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5">
                    <fmt:message key="inst.student.name.header">User name</fmt:message>
                </th>
                <th width="30%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5">
                    <fmt:message key="inst.student.email.header">Email</fmt:message>
                </th>
                <th width="30%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5">
                    <fmt:message key="inst.student.status.header">Status</fmt:message>
                </th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="student" items="${students}">
            <tr class="${student.clickerRegistered ? 'registered' : 'unregistered'} students_row data_row style1">
                <td align="center" class="user_name">${student.name}</td>
                <td align="center" class="user_email">${student.email}</td>
                <td align="center" class="clicker_status"><fmt:message key="inst.student.registered.${student.clickerRegistered}">Registered</fmt:message></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:when><c:otherwise>
    <div class="title"><fmt:message key="inst.courses.header">Courses Listing</fmt:message> (${coursesCount}):</div>
    <!-- course listing -->
    <table width="90%" border="1" cellspacing="0" cellpadding="0" 
        summary="<fmt:message key="inst.courses.table.summary" />">
        <thead>
            <tr class="courses_header header_row">
                <th width="70%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5">
                    <fmt:message key="inst.course">Course</fmt:message>
                </th>
                <th width="30%" scope="col" height="25" valign="middle" bgcolor="#e8e8e8" class="style5"></th>
            </tr>
        </thead>
        <tbody>
        <c:forEach var="course" items="${courses}">
            <tr class="courses_row data_row style1">
                <td align="center">${course.title}</td>
                <td align="center"><a href="${instPath}&courseId=${course.id}"><fmt:message key="inst.course.view.students" /></a></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:otherwise></c:choose>
</div>
