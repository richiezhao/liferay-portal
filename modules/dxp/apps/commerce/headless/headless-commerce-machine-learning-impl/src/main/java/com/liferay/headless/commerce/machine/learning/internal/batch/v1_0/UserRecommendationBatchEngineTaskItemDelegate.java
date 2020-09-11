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

package com.liferay.headless.commerce.machine.learning.internal.batch.v1_0;

import com.liferay.batch.engine.BaseBatchEngineTaskItemDelegate;
import com.liferay.batch.engine.BatchEngineTaskItemDelegate;
import com.liferay.batch.engine.pagination.Page;
import com.liferay.batch.engine.pagination.Pagination;
import com.liferay.commerce.machine.learning.recommendation.model.UserCommerceMLRecommendation;
import com.liferay.commerce.machine.learning.recommendation.service.UserCommerceMLRecommendationService;
import com.liferay.headless.commerce.machine.learning.dto.v1_0.UserRecommendation;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.io.Serializable;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	enabled = false, immediate = true,
	service = BatchEngineTaskItemDelegate.class
)
public class UserRecommendationBatchEngineTaskItemDelegate
	extends BaseBatchEngineTaskItemDelegate<UserRecommendation> {

	@Override
	public void createItem(
			UserRecommendation item, Map<String, Serializable> parameters)
		throws Exception {

		UserCommerceMLRecommendation userCommerceMLRecommendation =
			_userCommerceMLRecommendationService.create();

		userCommerceMLRecommendation.setAssetCategoryIds(
			ArrayUtil.toArray(item.getAssetCategoryIds()));
		userCommerceMLRecommendation.setCompanyId(
			contextCompany.getCompanyId());
		userCommerceMLRecommendation.setCreateDate(item.getCreateDate());
		userCommerceMLRecommendation.setEntryClassPK(item.getProductId());
		userCommerceMLRecommendation.setJobId(item.getJobId());
		userCommerceMLRecommendation.setRecommendedEntryClassPK(
			item.getRecommendedProductId());
		userCommerceMLRecommendation.setScore(item.getScore());

		_userCommerceMLRecommendationService.addUserCommerceMLRecommendation(
			userCommerceMLRecommendation);
	}

	@Override
	public Page<UserRecommendation> read(
			Filter filter, Pagination pagination, Sort[] sorts,
			Map<String, Serializable> parameters, String search)
		throws Exception {

		return null;
	}

	@Reference
	private UserCommerceMLRecommendationService
		_userCommerceMLRecommendationService;

}