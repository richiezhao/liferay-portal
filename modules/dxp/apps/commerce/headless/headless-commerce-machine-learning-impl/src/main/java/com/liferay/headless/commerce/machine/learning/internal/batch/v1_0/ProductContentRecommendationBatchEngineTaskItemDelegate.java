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
import com.liferay.commerce.machine.learning.recommendation.model.ProductContentCommerceMLRecommendation;
import com.liferay.commerce.machine.learning.recommendation.service.ProductContentCommerceMLRecommendationService;
import com.liferay.headless.commerce.machine.learning.dto.v1_0.ProductContentRecommendation;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.Filter;

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
public class ProductContentRecommendationBatchEngineTaskItemDelegate
	extends BaseBatchEngineTaskItemDelegate<ProductContentRecommendation> {

	@Override
	public void createItem(
			ProductContentRecommendation item,
			Map<String, Serializable> parameters)
		throws Exception {

		ProductContentCommerceMLRecommendation
			productContentCommerceMLRecommendation =
				_productContentCommerceMLRecommendationService.create();

		productContentCommerceMLRecommendation.setCompanyId(
			contextCompany.getCompanyId());
		productContentCommerceMLRecommendation.setCreateDate(
			item.getCreateDate());
		productContentCommerceMLRecommendation.setEntryClassPK(
			item.getProductId());
		productContentCommerceMLRecommendation.setJobId(item.getJobId());
		productContentCommerceMLRecommendation.setRank(item.getRank());
		productContentCommerceMLRecommendation.setRecommendedEntryClassPK(
			item.getRecommendedProductId());
		productContentCommerceMLRecommendation.setScore(item.getScore());

		_productContentCommerceMLRecommendationService.
			addProductContentCommerceMLRecommendation(
				productContentCommerceMLRecommendation);
	}

	@Override
	public Page<ProductContentRecommendation> read(
			Filter filter, Pagination pagination, Sort[] sorts,
			Map<String, Serializable> parameters, String search)
		throws Exception {

		return null;
	}

	@Reference
	private ProductContentCommerceMLRecommendationService
		_productContentCommerceMLRecommendationService;

}