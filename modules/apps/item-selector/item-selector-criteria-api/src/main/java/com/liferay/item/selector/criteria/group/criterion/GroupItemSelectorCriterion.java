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

package com.liferay.item.selector.criteria.group.criterion;

import com.liferay.item.selector.BaseItemSelectorCriterion;

/**
 * @author Adolfo Pérez
 */
public class GroupItemSelectorCriterion extends BaseItemSelectorCriterion {

	public GroupItemSelectorCriterion() {
	}

	public GroupItemSelectorCriterion(boolean privateLayout) {
		_privateLayout = privateLayout;
	}

	public String getPortletId() {
		return _portletId;
	}

	public String getTarget() {
		return _target;
	}

	public boolean isAllowNavigation() {
		return _allowNavigation;
	}

	public boolean isIncludeAllVisibleGroups() {
		return _includeAllVisibleGroups;
	}

	public boolean isIncludeFormsSite() {
		return _includeFormsSite;
	}

	public boolean isIncludeRecentSites() {
		return _includeRecentSites;
	}

	public boolean isIncludeUserPersonalSite() {
		return _includeUserPersonalSite;
	}

	public boolean isPrivateLayout() {
		return _privateLayout;
	}

	public void setAllowNavigation(boolean allowNavigation) {
		_allowNavigation = allowNavigation;
	}

	public void setIncludeAllVisibleGroups(boolean includeAllVisibleGroups) {
		_includeAllVisibleGroups = includeAllVisibleGroups;
	}

	public void setIncludeFormsSite(boolean includeFormsSite) {
		_includeFormsSite = includeFormsSite;
	}

	public void setIncludeRecentSites(boolean includeRecentSites) {
		_includeRecentSites = includeRecentSites;
	}

	public void setIncludeUserPersonalSite(boolean includeUserPersonalSite) {
		_includeUserPersonalSite = includeUserPersonalSite;
	}

	public void setPortletId(String portletId) {
		_portletId = portletId;
	}

	public void setPrivateLayout(boolean privateLayout) {
		_privateLayout = privateLayout;
	}

	public void setTarget(String target) {
		_target = target;
	}

	private boolean _allowNavigation = true;
	private boolean _includeAllVisibleGroups;
	private boolean _includeFormsSite;
	private boolean _includeRecentSites = true;
	private boolean _includeUserPersonalSite;
	private String _portletId;
	private boolean _privateLayout;
	private String _target;

}