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
<% controller.processRegistration(pageContext, request); %>
<c:if test="${isAdmin || isInstructor}">
<ul class="navIntraTool actionToolBar nav_items">
    <li class="firstToolBarItem nav_item"><span class="current"><fmt:message key="reg.title" /></span></li>
    <c:choose><c:when test="${isAdmin}">
        <li class="nav_item"><a href="${adminPath}"><fmt:message key="admin.title" /></a></li>
    </c:when><c:otherwise>
        <li class="nav_item"><a href="${instPath}"><fmt:message key="inst.title" /></a></li>
        <c:if test="${ssoEnabled}">
        <li class="nav_item"><a href="${instSSOPath}"><fmt:message key="inst.sso.link" /></a></li>
        </c:if>
    </c:otherwise></c:choose>
</ul>
</c:if>

<h3 class="insColor insBak insBorder page_header">&nbsp;<fmt:message key="app.iclicker">iClicker</fmt:message> <fmt:message key="reg.title">Sample Title</fmt:message></h3>
<!-- show messages if there are any to show -->
<jsp:include page="/views/userMsgs.jsp"></jsp:include>

<%-- commented out SSO message -AZ
<c:if test="${ssoEnabled && isInstructor}">
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
    <div class="columns_container">
        <div class="left_column">
            <p><fmt:message key="reg.remote.instructions">Enter the 8-character remote ID below to register your i>clicker remote. You may register multiple remotes or remove a remote at any time.</fmt:message></p>

            <form method="post" id="registerForm" style="display: inline;">
                <input type="hidden" name="view" value="${regView}" />
                <input type="hidden" name="register" value="true" />
                <p class="highlighted">
                    <strong><fmt:message key="reg.remote.id.enter">Enter Your i>clicker Remote ID</fmt:message>:</strong> 
                    <input name="clickerId" type="text" size="10" maxlength="8" value="${clickerIdText}" />
                    <input type="submit" class="registerButton" value="<fmt:message key="app.register">Register</fmt:message>" 
                        alt="<fmt:message key="reg.register.submit.alt" />" />
                </p>
            </form>

            <c:if test="${fn:length(regs) > 0}">
            <table class="remotes" summary="<fmt:message key="reg.registration.table.summary" />">
                <colgroup>
                    <col width="40%" />
                    <col width="40%" />
                    <col />
                </colgroup>
                <tr>
                    <th><fmt:message key="reg.remote.id.header">Registered Remote ID</fmt:message></th>
                    <th><fmt:message key="reg.registered.date.header">Date Registered</fmt:message></th>
                    <th>&nbsp;</th>
                </tr>
                <c:forEach var="registration" items="${regs}">
                <tr>
                    <td>${registration.clickerId}</td>
                    <td><fmt:formatDate value="${registration.dateCreated}" pattern="MMM dd, yyyy"/></td>
                    <td>
                        <form method="post" style="display: inline;">
                            <input type="hidden" name="view" value="${regView}" />
                            <input type="hidden" name="remove" value="remove" />
                            <input type="hidden" name="registrationId" value="${registration.id}" />
                            <input type="submit" class="small" value="<fmt:message key="app.remove">Remove</fmt:message>" alt="<fmt:message key="reg.remove.submit.alt" />" />
                        </form>
                    </td>
                </tr>
                </c:forEach>
            </table>
            </c:if>

            <c:if test="${fn:length(belowMessages) > 0}">
            <!-- registration below messages area -->
            <div class="registration_below_messages_holder style5" style="margin-top: 1em;">
                <div class="registration_below_messages">
                    <c:forEach var="message" items="${belowMessages}">
                    <p class="registration_below_message">${message}</p>
                    </c:forEach>
                </div>
            </div>
            </c:if>
        </div>

        <div class="right_column">
            <h3><fmt:message key="reg.remote.faqs">Remote Registration FAQs</fmt:message></h3>
            
            <div id="accordion">
                <h3><a href="#"><fmt:message key="reg.remote.faq1.question">Where do I find my remote ID?</fmt:message></a></h3>
                <div>
                <fmt:message key="reg.remote.faq1.answer">Your i>clicker remote ID is printed on a sticker located on the back of your remote. The ID is the 8-character code below the barcode.</fmt:message>
                <img src="images/clickers.png" alt="<fmt:message key="reg.remote.faq1.image.alt" />" />
                </div>

                <h3><a href="#"><fmt:message key="reg.remote.faq2.question">What do I do if I cannot read the ID printed on my remote?</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq2.answer">If your remote ID has rubbed off or is illegible, go to http://www.iclicker.com/support/findclickerID/ for instructions on how to recover your remote ID.</fmt:message></div>

                <h3><a href="#"><fmt:message key="reg.remote.faq3.question">Question</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq3.answer">[Content Here]</fmt:message></div>

                <h3><a href="#"><fmt:message key="reg.remote.faq4.question">Question</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq4.answer">[Content Here]</fmt:message></div>

                <h3><a href="#"><fmt:message key="reg.remote.faq5.question">Question</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq5.answer">[Content Here]</fmt:message></div>

                <h3><a href="#"><fmt:message key="reg.remote.faq6.question">Question</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq6.answer">[Content Here]</fmt:message></div>

                <h3><a href="#"><fmt:message key="reg.remote.faq7.question">Question</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq7.answer">[Content Here]</fmt:message></div>

                <h3><a href="#"><fmt:message key="reg.remote.faq8.question">Question</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq8.answer">[Content Here]</fmt:message></div>

                <h3><a href="#"><fmt:message key="reg.remote.faq9.question">Question</fmt:message></a></h3>
                <div><fmt:message key="reg.remote.faq9.answer">[Content Here]</fmt:message></div>

            </div>

        </div>
    </div>
</div>

<script>
    $(document).ready(function() {
        $("#accordion").accordion({ active: 0, 
            alwaysOpen: false, 
            animated: false, 
            autoHeight: false,
            collapsible: true,
            heightStyle: "content"
        });
    });
</script>

