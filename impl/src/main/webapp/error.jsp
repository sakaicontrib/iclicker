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
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>ERROR: Unhandled failure</title>
</head>
<body>
<h1>We're sorry but an error has occurred with the application!</h1>
<h2>This should not have happened and indicates a bug in the application in most cases. 
It may also indicate that an action was attempted which is not allowed for the current user attempting it.</h2>
<% 
String code = "ERR_"+System.currentTimeMillis()+"_"+exception.hashCode();
System.err.println("Error code for failure (see above in logs): " + code);
exception.printStackTrace();
%>
<p>Please click Back in your browser and attempt to repeat your previous action. 
If the failure occurs again then you should reference this 
error code when contacting a system administrator: <%= code %></p>
</body>
</html>