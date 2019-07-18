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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" import="org.sakaiproject.iclicker.tool.ToolController" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%><%
    // get the messages
    pageContext.setAttribute("infos", ToolController.getMessages(pageContext, ToolController.KEY_INFO));
    pageContext.setAttribute("alerts", ToolController.getMessages(pageContext, ToolController.KEY_ERROR));
%>
<c:if test="${fn:length(alerts) > 0}">
<div class="alertMessage user_messages alert_messages">
    <ul class="messages_list">
        <c:forEach var="message" items="${alerts}">
        <li class="user_message info_message">${message}</li>
        </c:forEach>
    </ul>
</div>
</c:if>
<c:if test="${fn:length(infos) > 0}">
<div class="information user_messages info_messages">
    <ul class="messages_list">
        <c:forEach var="message" items="${infos}">
        <li class="user_message alert_message">${message}</li>
        </c:forEach>
    </ul>
</div>
</c:if>
