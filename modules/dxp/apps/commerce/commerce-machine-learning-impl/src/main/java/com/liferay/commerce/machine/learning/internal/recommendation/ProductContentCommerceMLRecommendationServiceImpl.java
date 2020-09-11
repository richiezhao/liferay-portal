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

package com.liferay.commerce.machine.learning.internal.recommendation;

import com.liferay.commerce.machine.learning.internal.recommendation.constants.CommerceMLRecommendationField;
import com.liferay.commerce.machine.learning.internal.recommendation.model.ProductContentCommerceMLRecommendationImpl;
import com.liferay.commerce.machine.learning.internal.search.api.CommerceMLIndexer;
import com.liferay.commerce.machine.learning.recommendation.model.ProductContentCommerceMLRecommendation;
import com.liferay.commerce.machine.learning.recommendation.service.ProductContentCommerceMLRecommendationService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.SortFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.search.engine.adapter.search.SearchSearchRequest;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Ferrari
 */
@Component(
	enabled = false, immediate = true,
	service = ProductContentCommerceMLRecommendationService.class
)
public class ProductContentCommerceMLRecommendationServiceImpl
	extends BaseCommerceMLRecommendationServiceImpl
		<ProductContentCommerceMLRecommendation>
	implements ProductContentCommerceMLRecommendationService {

	@Override
	public ProductContentCommerceMLRecommendation
			addProductContentCommerceMLRecommendation(
				ProductContentCommerceMLRecommendation
					productContentCommerceMLRecommendation)
		throws PortalException {

		return addCommerceMLRecommendation(
			productContentCommerceMLRecommendation,
			_commerceMLIndexer.getIndexName(
				productContentCommerceMLRecommendation.getCompanyId()),
			_commerceMLIndexer.getDocumentType());
	}

	@Override
	public ProductContentCommerceMLRecommendation create() {
		return new ProductContentCommerceMLRecommendationImpl();
	}

	@Override
	public List<ProductContentCommerceMLRecommendation>
			getProductContentCommerceMLRecommendations(
				long companyId, long cpDefinition)
		throws PortalException {

		SearchSearchRequest searchSearchRequest = getSearchSearchRequest(
			_commerceMLIndexer.getIndexName(companyId), companyId,
			cpDefinition);

		Sort rankSort = SortFactoryUtil.create(
			CommerceMLRecommendationField.RANK, Sort.INT_TYPE, false);

		searchSearchRequest.setSorts(new Sort[] {rankSort});

		return getSearchResults(searchSearchRequest);
	}

	@Override
	protected Document toDocument(
		ProductContentCommerceMLRecommendation model) {

		Document document = getBaseDocument(model);

		document.addNumber(CommerceMLRecommendationField.RANK, model.getRank());

		return document;
	}

	@Override
	protected ProductContentCommerceMLRecommendation toModel(
		Document document) {

		ProductContentCommerceMLRecommendation
			productContentCommerceMLRecommendation =
				getBaseCommerceMLRecommendationModel(
					new ProductContentCommerceMLRecommendationImpl(), document);

		productContentCommerceMLRecommendation.setRank(
			GetterUtil.getInteger(
				document.get(CommerceMLRecommendationField.RANK)));

		return productContentCommerceMLRecommendation;
	}

	@Reference(
		target = "(component.name=com.liferay.commerce.machine.learning.internal.recommendation.search.index.ProductContentCommerceMLRecommendationIndexer)"
	)
	private CommerceMLIndexer _commerceMLIndexer;

}