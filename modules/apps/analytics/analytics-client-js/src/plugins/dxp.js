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

import {
	MARK_LOAD_EVENT_START,
	MARK_NAVIGATION_START,
	MARK_PAGE_LOAD_TIME,
	MARK_VIEW_DURATION,
} from '../utils/constants';
import {createMark, getDuration} from '../utils/performance';

const pageApplicationId = 'Page';

/**
 * Plugin function that registers listeners related to DXP
 * @param {Object} analytics The Analytics client
 */

function dxp(analytics) {

	/**
	 * Sends view duration information on the window unload event
	 */
	function sendUnloadEvent() {
		analytics.send('pageUnloaded', pageApplicationId, {
			viewDuration: getDuration(
				MARK_VIEW_DURATION,
				MARK_NAVIGATION_START
			),
		});
	}

	/**
	 * Sends page load information on the endNavigate event when SPA is enabled on DXP
	 */
	function sendLoadEvent() {
		analytics.send('pageLoaded', pageApplicationId, {
			pageLoadTime: getDuration(
				MARK_PAGE_LOAD_TIME,
				MARK_LOAD_EVENT_START,
				MARK_NAVIGATION_START
			),
		});
	}

	if (window.Liferay && window.Liferay.SPA) {
		const loadingStartMarks = window.performance.getEntriesByName(
			MARK_LOAD_EVENT_START
		);

		createMark(MARK_NAVIGATION_START);

		if (!loadingStartMarks.length) {
			const createLoadMark = createMark.bind(null, MARK_LOAD_EVENT_START);

			createMark(MARK_LOAD_EVENT_START);
			window.Liferay.on('beforeNavigate', createLoadMark);
		}

		if (document.readyState === 'complete') {
			sendLoadEvent();
		}

		window.Liferay.once('beforeNavigate', sendUnloadEvent);
	}
}

export {dxp};
export default dxp;
