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
<?xml version="1.0" encoding="UTF-8" ?>
<%@page import="org.sakaiproject.iclicker.logic.IClickerLogic"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" errorPage="error.jsp"%><%@ include file="/views/includeOnce.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<%
    String toolBaseCSS = org.sakaiproject.iclicker.utils.CSSUtils.getCssToolBase();
    String toolCSS = org.sakaiproject.iclicker.utils.CSSUtils.getCssToolSkin((String)null);
%>
<script src="/library/js/headscripts.js" language="JavaScript" type="text/javascript"></script>
<script src="javascript/jquery-1.11.0.min.js" language="JavaScript" type="text/javascript"></script>
<script src="javascript/jquery-ui-1.10.4.custom.min.js" language="JavaScript" type="text/javascript"></script>
<script src="javascript/iclicker.js" language="JavaScript" type="text/javascript"></script>
<link media="all" href="<%=toolBaseCSS%>" rel="stylesheet" type="text/css" />
<link media="all" href="<%=toolCSS%>" rel="stylesheet" type="text/css" />
<link media="all" href="css/jquery-ui-1.10.4.custom.min.css" rel="stylesheet" type="text/css" />
<link media="all" href="css/iclicker.css" rel="stylesheet" type="text/css" />
<title><fmt:message key="app.iclicker">iClicker</fmt:message> <fmt:message key="app.title">Sample Title</fmt:message></title>
</head>
<body onload="<%=request.getAttribute("sakai.html.body.onload")%>">
<div class="iclicker">
<div class="portletBody">
<jsp:include page="${viewPath}"></jsp:include>
</div>
<div class="iclicker_version">Version <%=IClickerLogic.VERSION%> (<%=IClickerLogic.VERSION_DATE%>)</div>
</div>
</body>
</html>
