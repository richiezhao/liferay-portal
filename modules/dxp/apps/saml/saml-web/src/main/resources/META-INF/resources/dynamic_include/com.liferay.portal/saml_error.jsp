<%--
/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */
--%>

<%@ include file="/dynamic_include/init.jsp" %>

<%
String samlSubjectScreenName = (String)request.getAttribute(SamlWebKeys.SAML_SUBJECT_NAME_ID);
%>

<liferay-util:buffer
	var="msg"
>
	<liferay-ui:message arguments='<%= "<strong>" + HtmlUtil.escape(samlSubjectScreenName) + "</strong>" %>' key="your-user-x-could-not-be-logged-in" />

	<c:choose>
		<c:when test='<%= SessionMessages.contains(request, "ContactNameException") %>'>
			<liferay-ui:message key="your-contact-name-is-incomplete-or-invalid" />
		</c:when>
		<c:when test='<%= SessionMessages.contains(request, "MustNotUseCompanyMx") %>'>
			<liferay-ui:message key="the-email-address-associated-with-your-saml-account-cannot-be-used-to-register-a-new-user-because-its-email-domain-is-reserved" />
		</c:when>
		<c:when test='<%= SessionMessages.contains(request, "SubjectException") %>'>
			<liferay-ui:message key="only-known-users-are-allowed-to-sign-in-using-saml" />
		</c:when>
		<c:when test='<%= SessionMessages.contains(request, "UserEmailAddressException") %>'>
			<liferay-ui:message key="your-email-address-is-incomplete-or-invalid" />
		</c:when>
		<c:when test='<%= SessionMessages.contains(request, "UserScreenNameException") %>'>
			<liferay-ui:message key="your-screen-name-is-incomplete-or-invalid" />
		</c:when>
	</c:choose>

	<a href="<%= themeDisplay.getURLSignIn() %>"><liferay-ui:message arguments='<%= "<strong>" + HtmlUtil.escapeAttribute(samlSubjectScreenName) + "</strong>" %>' key="not-x" /></a>
</liferay-util:buffer>

<aui:script>
	Liferay.Util.openToast({
		message: '<%= HtmlUtil.escapeJS(msg) %>',
		type: 'warning',
	});
</aui:script>