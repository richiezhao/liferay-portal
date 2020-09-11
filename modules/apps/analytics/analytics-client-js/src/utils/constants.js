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

export const DEBOUNCE = 1500;

export const FLUSH_INTERVAL = 2000;

// Limit of a queue localStorage size in kilobytes.

export const QUEUE_STORAGE_LIMIT = 512;

//Queue priority

export const QUEUE_PRIORITY_DEFAULT = 1;

export const QUEUE_PRIORITY_IDENTITY = 10;

// Local Storage keys

export const STORAGE_KEY_CONTEXTS = 'ac_client_context';

export const STORAGE_KEY_EVENTS = 'ac_client_batch';

export const STORAGE_KEY_IDENTITY = 'ac_client_identity';

export const STORAGE_KEY_MESSAGES = 'ac_message_queue';

export const STORAGE_KEY_MESSAGE_IDENTITY = 'ac_message_queue_identity';

export const STORAGE_KEY_USER_ID = 'ac_client_user_id';

// Request Constants

export const LIMIT_FAILED_ATTEMPTS = 7;

export const REQUEST_TIMEOUT = 5000;

//DXP Timing

export const MARK_LOAD_EVENT_START = 'loadEventStartSPA';

export const MARK_NAVIGATION_START = 'navigationStartSPA';

export const MARK_PAGE_LOAD_TIME = 'pageLoadTimeSPA';

export const MARK_VIEW_DURATION = 'viewDurationSPA';

// Read metrics Constants

export const READ_CHARS_PER_MIN = 500;

export const READ_LOGOGRAPHIC_LANGUAGES = new Set(['ja', 'ko', 'zh']);

export const READ_MINIMUM_SCROLL_DEPTH = 75;

export const READ_TIME_FACTOR = 0.75;

export const READ_WORDS_PER_MIN = 265;
