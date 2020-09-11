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

package com.liferay.portal.search.elasticsearch7.internal.util;

import com.liferay.petra.io.unsync.UnsyncByteArrayOutputStream;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.search.elasticsearch7.configuration.RESTClientLoggerLevel;

import java.io.IOException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput;

/**
 * @author Adam Brandizzi
 */
public class SearchLogHelperImpl implements SearchLogHelper {

	@Override
	public void logActionResponse(Log log, ActionResponse actionResponse) {
		if (!log.isInfoEnabled()) {
			return;
		}

		UnsyncByteArrayOutputStream unsyncByteArrayOutputStream =
			new UnsyncByteArrayOutputStream();

		try {
			actionResponse.writeTo(
				new OutputStreamStreamOutput(unsyncByteArrayOutputStream));
		}
		catch (IOException ioException) {
			throw new RuntimeException(ioException);
		}

		String string = unsyncByteArrayOutputStream.toString();

		log.info(string.trim());
	}

	@Override
	public void logActionResponse(Log log, BulkResponse bulkResponse) {
		if (bulkResponse.hasFailures()) {
			log.error(bulkResponse.buildFailureMessage());
		}

		logActionResponse(log, (ActionResponse)bulkResponse);
	}

	@Override
	public void setRESTClientLoggerLevel(
		RESTClientLoggerLevel restClientLoggerLevel) {

		org.apache.commons.logging.Log log = LogFactory.getLog(
			RestClient.class);

		if (log instanceof Log4JLogger) {
			Log4JLogger log4JLogger = (Log4JLogger)log;

			Logger logger = log4JLogger.getLogger();

			logger.setLevel(Level.toLevel(restClientLoggerLevel.name()));
		}
	}

}