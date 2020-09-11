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

package com.liferay.commerce.product.definitions.web.internal.frontend;

import com.liferay.commerce.pricing.model.CommercePricingClass;
import com.liferay.commerce.pricing.model.CommercePricingClassCPDefinitionRel;
import com.liferay.commerce.pricing.service.CommercePricingClassCPDefinitionRelService;
import com.liferay.commerce.product.definitions.web.internal.model.ProductPricingClass;
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.frontend.taglib.clay.data.set.ClayDataSetActionProvider;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItem;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.DropdownItemListBuilder;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.portlet.PortletProvider;
import com.liferay.portal.kernel.portlet.PortletProviderUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;
import com.liferay.portal.kernel.util.Portal;

import java.util.List;

import javax.portlet.PortletURL;
import javax.portlet.WindowStateException;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Riccardo Alberti
 */
@Component(
	enabled = false, immediate = true,
	property = "clay.data.provider.key=" + CommerceProductDataSetConstants.COMMERCE_DATA_SET_KEY_PRODUCT_PRICING_CLASSES,
	service = ClayDataSetActionProvider.class
)
public class CommerceProductPricingClassDataSetActionProvider
	implements ClayDataSetActionProvider {

	@Override
	public List<DropdownItem> getDropdownItems(
			HttpServletRequest httpServletRequest, long groupId, Object model)
		throws PortalException {

		ProductPricingClass productPricingClass = (ProductPricingClass)model;

		return DropdownItemListBuilder.add(
			() -> _commercePricingClassModelResourcePermission.contains(
				PermissionThreadLocal.getPermissionChecker(),
				productPricingClass.getPricingClassId(), ActionKeys.UPDATE),
			dropdownItem -> {
				dropdownItem.setHref(
					_getPricingClassEditURL(
						productPricingClass.getPricingClassId(),
						httpServletRequest));
				dropdownItem.setLabel(
					LanguageUtil.get(httpServletRequest, "edit"));
				dropdownItem.setTarget("sidePanel");
			}
		).add(
			() -> _commercePricingClassModelResourcePermission.contains(
				PermissionThreadLocal.getPermissionChecker(),
				productPricingClass.getPricingClassId(), ActionKeys.DELETE),
			dropdownItem -> {
				dropdownItem.putData("method", "delete");
				dropdownItem.setHref(
					_getProductPricingClassDeleteURL(
						productPricingClass.getPricingClassId(),
						productPricingClass.getCpDefinitionId()));
				dropdownItem.setLabel(
					LanguageUtil.get(httpServletRequest, "remove"));
				dropdownItem.setTarget("async");
			}
		).build();
	}

	private PortletURL _getPricingClassEditURL(
			long pricingClassId, HttpServletRequest httpServletRequest)
		throws PortalException {

		PortletURL portletURL = PortletProviderUtil.getPortletURL(
			httpServletRequest, CPDefinition.class.getName(),
			PortletProvider.Action.MANAGE);

		portletURL.setParameter(
			"mvcRenderCommandName", "editCommercePricingClass");
		portletURL.setParameter(
			"commercePricingClassId", String.valueOf(pricingClassId));

		try {
			portletURL.setWindowState(LiferayWindowState.POP_UP);
		}
		catch (WindowStateException windowStateException) {
			_log.error(windowStateException, windowStateException);
		}

		return portletURL;
	}

	private String _getProductPricingClassDeleteURL(
			long pricingClassId, long cpDefinitionId)
		throws PortalException {

		CommercePricingClassCPDefinitionRel
			commercePricingClassCPDefinitionRel =
				_commercePricingClassCPDefinitionRelService.
					fetchCommercePricingClassCPDefinitionRel(
						pricingClassId, cpDefinitionId);

		return "/o/headless-commerce-admin-catalog/v1.0" +
			"/product-group-products/" +
				commercePricingClassCPDefinitionRel.
					getCommercePricingClassCPDefinitionRelId();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CommerceProductPricingClassDataSetActionProvider.class);

	@Reference
	private CommercePricingClassCPDefinitionRelService
		_commercePricingClassCPDefinitionRelService;

	@Reference(
		target = "(model.class.name=com.liferay.commerce.pricing.model.CommercePricingClass)"
	)
	private ModelResourcePermission<CommercePricingClass>
		_commercePricingClassModelResourcePermission;

	@Reference
	private Portal _portal;

}