/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.users.admin.search.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistry;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.search.test.util.SearchStreamUtil;
import com.liferay.portal.search.test.util.SearchTestRule;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.users.admin.kernel.util.UsersAdmin;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jesse Yeh
 */
@RunWith(Arquillian.class)
public class UserSearchTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		Group group = GroupTestUtil.addGroup();

		Assert.assertNotNull(
			"RoleLocalServiceUtil must be resolved for " +
				"UserTestUtil.addGroupAdminUser",
			roleLocalService);
		Assert.assertNotNull(
			"UserGroupRoleLocalServiceUtil must be resolved for " +
				"UserTestUtil.addGroupAdminUser",
			userGroupRoleLocalService);

		User groupAdminUser = UserTestUtil.addGroupAdminUser(group);

		User groupMemberUser = UserTestUtil.addGroupUser(
			group, RoleConstants.SITE_MEMBER);

		User nongroupMemberUser = UserTestUtil.addUser();

		_group = group;

		_groupAdminUser = groupAdminUser;

		_groupMemberUser = groupMemberUser;

		_nongroupMemberUser = nongroupMemberUser;
	}

	@Test
	public void testSearchUsersAsGroupAdmin() throws Exception {
		PermissionThreadLocal.setPermissionChecker(
			PermissionCheckerFactoryUtil.create(_groupAdminUser));

		String keywords = "";
		int status = 0;
		LinkedHashMap<String, Object> userParams =
			LinkedHashMapBuilder.<String, Object>put(
				Field.GROUP_ID, Long.valueOf(_groupAdminUser.getGroupIds()[0])
			).build();
		int start = QueryUtil.ALL_POS;
		int end = QueryUtil.ALL_POS;

		Assert.assertNotNull(
			"IndexerRegistryUtil must be resolved for " +
				"UserLocalServiceImpl.search",
			indexerRegistry);
		Assert.assertNotNull(
			"Indexer<User> must be resolved for UserLocalServiceImpl.search",
			indexer);

		Hits hits = userLocalService.search(
			TestPropsValues.getCompanyId(), keywords, status, userParams, start,
			end, new Sort());

		List<Long> testUserIdsList = Arrays.asList(
			_groupAdminUser.getUserId(), _groupMemberUser.getUserId(),
			_nongroupMemberUser.getUserId());

		Stream<Document> documentsStream = SearchStreamUtil.stream(
			hits.toList());

		Set<Long> hitsUserIdsSet = documentsStream.map(
			document -> Long.valueOf(document.get(Field.USER_ID))
		).collect(
			Collectors.toSet()
		);

		assertContainsAll(testUserIdsList, hitsUserIdsSet);

		List<User> users = usersAdmin.getUsers(hits);

		Assert.assertNotNull(
			"null should NOT be returned but one userId was a stale document " +
				"with no model in persistence: " + hitsUserIdsSet,
			users);

		Stream<User> usersStream = SearchStreamUtil.stream(users);

		assertContainsAll(
			testUserIdsList,
			usersStream.map(
				User::getUserId
			).collect(
				Collectors.toSet()
			));
	}

	@Rule
	public SearchTestRule searchTestRule = new SearchTestRule();

	protected void assertContainsAll(
		List<Long> expectedList, Set<Long> actualSet) {

		Assert.assertTrue(
			StringBundler.concat(
				actualSet, " should contain all ", expectedList),
			actualSet.containsAll(expectedList));
	}

	@Inject(filter = "indexer.class.name=com.liferay.portal.kernel.model.User")
	protected Indexer<User> indexer;

	@Inject
	protected IndexerRegistry indexerRegistry;

	@Inject
	protected RoleLocalService roleLocalService;

	@Inject
	protected UserGroupRoleLocalService userGroupRoleLocalService;

	@Inject
	protected UserLocalService userLocalService;

	@Inject
	protected UsersAdmin usersAdmin;

	@DeleteAfterTestRun
	private Group _group;

	@DeleteAfterTestRun
	private User _groupAdminUser;

	@DeleteAfterTestRun
	private User _groupMemberUser;

	@DeleteAfterTestRun
	private User _nongroupMemberUser;

}