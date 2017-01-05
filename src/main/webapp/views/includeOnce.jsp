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
<%@ include file="/views/includeHeader.jsp" %><%
WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(application);
pageContext.setAttribute("applicationContext", applicationContext, PageContext.REQUEST_SCOPE);
// Get the backing bean from the spring context
ToolController controller = (ToolController) applicationContext.getBean(ToolController.class.getName());
pageContext.setAttribute("controller", controller, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("messageSource", controller.getMessageSource(), PageContext.REQUEST_SCOPE);
pageContext.setAttribute("locale", controller.getMessageLocale(pageContext), PageContext.REQUEST_SCOPE);
pageContext.setAttribute("messageBundle", ToolController.MESSAGE_BUNDLE, PageContext.REQUEST_SCOPE);
String view = controller.getValidView( request.getParameter("view") );
pageContext.setAttribute("view", view, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("regView", ToolController.VIEW_REGISTRATION, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("instView", ToolController.VIEW_INSTRUCTOR, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("instSSOView", ToolController.VIEW_INSTRUCTOR_SSO, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("adminView", ToolController.VIEW_ADMIN, PageContext.REQUEST_SCOPE);
String viewPath = "views/"+view+".jsp";
//System.out.println("INFO: path: "+viewPath+"  view:"+view);
pageContext.setAttribute("viewPath", viewPath, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("regPath", "index.jsp", PageContext.REQUEST_SCOPE); // default is reg view
pageContext.setAttribute("instPath", "index.jsp?view="+ToolController.VIEW_INSTRUCTOR, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("instSSOPath", "index.jsp?view="+ToolController.VIEW_INSTRUCTOR_SSO, PageContext.REQUEST_SCOPE);
pageContext.setAttribute("adminPath", "index.jsp?view="+ToolController.VIEW_ADMIN, PageContext.REQUEST_SCOPE);
// permissions needed on all pages
pageContext.setAttribute("isAdmin", controller.isAdmin(), PageContext.REQUEST_SCOPE);
// <fmt:setLocale value="${loc}" /> - allow overriding the one from the browser
// <fmt:setBundle basename="i18n.Messages" /> - ideally spring would take care of this
%><fmt:setLocale value="${locale}" /><fmt:setBundle  basename="${messageBundle}" />