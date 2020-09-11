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

package com.liferay.source.formatter.checks;

import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.tools.ToolsUtil;
import com.liferay.source.formatter.checks.util.JSPSourceUtil;
import com.liferay.source.formatter.util.SourceFormatterUtil;

import java.io.IOException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hugo Huijser
 */
public abstract class BaseJSPTermsCheck extends BaseFileCheck {

	@Override
	public void setAllFileNames(List<String> allFileNames) {
		_allFileNames = allFileNames;
	}

	protected Map<String, String> getContentsMap() {
		return _contentsMap;
	}

	protected boolean hasUnusedJSPTerm(
		String fileName, String regex, String type,
		Set<String> checkedForIncludesFileNames, Set<String> includeFileNames,
		Map<String, String> contentsMap) {

		return hasUnusedJSPTerm(
			fileName, null, regex, type, checkedForIncludesFileNames,
			includeFileNames, contentsMap);
	}

	protected boolean hasUnusedJSPTerm(
		String fileName, String content, String regex, String type,
		Set<String> checkedForIncludesFileNames, Set<String> includeFileNames,
		Map<String, String> contentsMap) {

		includeFileNames.add(fileName);

		Set<String> checkedForUnusedJSPTerm = new HashSet<>();

		return !_isJSPTermRequired(
			fileName, content, regex, type, checkedForUnusedJSPTerm,
			checkedForIncludesFileNames, includeFileNames, contentsMap);
	}

	protected synchronized void populateContentsMap(
			String fileName, String content)
		throws IOException {

		if (_contentsMap != null) {
			return;
		}

		String[] excludes = {"**/null.jsp", "**/tools/**"};

		List<String> allJSPFileNames = SourceFormatterUtil.filterFileNames(
			_allFileNames, excludes,
			new String[] {"**/*.jsp", "**/*.jspf", "**/*.tag"},
			getSourceFormatterExcludes(), true);

		_contentsMap = JSPSourceUtil.getContentsMap(allJSPFileNames);

		// When running tests, the contentsMap is empty, because the file
		// extension of the test files is *.testjsp

		if (_contentsMap.isEmpty()) {
			_contentsMap.put(fileName, content);
		}
	}

	protected void put(String fileName, String content) {
		_contentsMap.put(fileName, content);
	}

	private boolean _isJSPTermRequired(
		String fileName, String content, String regex, String type,
		Set<String> checkedForUnusedJSPTerm,
		Set<String> checkedForIncludesFileNames, Set<String> includeFileNames,
		Map<String, String> contentsMap) {

		if (checkedForUnusedJSPTerm.contains(fileName)) {
			return false;
		}

		checkedForUnusedJSPTerm.add(fileName);

		if (content == null) {
			content = contentsMap.get(fileName);
		}

		if (Validator.isNull(content)) {
			return false;
		}

		int count = 0;

		Pattern pattern = Pattern.compile(regex);

		Matcher matcher = pattern.matcher(content);

		while (matcher.find()) {
			if (type.equals("taglib")) {
				count++;

				continue;
			}

			int x = matcher.start() + 1;

			if (JSPSourceUtil.isJavaSource(content, x)) {
				if (!ToolsUtil.isInsideQuotes(content, x)) {
					count++;
				}

				continue;
			}

			String line = StringUtil.trim(
				getLine(content, getLineNumber(content, matcher.start())));

			if (line.startsWith("function ")) {
				continue;
			}

			int y = content.lastIndexOf("<%", x) + 2;
			int z = content.lastIndexOf("%>", x);

			if ((y == 1) || (z > y)) {
				continue;
			}

			z = content.indexOf("%>", x);

			if ((z == -1) ||
				(getLineNumber(content, y) != getLineNumber(content, z)) ||
				!ToolsUtil.isInsideQuotes(content.substring(y, z), x - y)) {

				count++;
			}
		}

		if ((count > 1) ||
			((count == 1) &&
			 (!type.equals("variable") ||
			  (checkedForUnusedJSPTerm.size() > 1)))) {

			return true;
		}

		if (!checkedForIncludesFileNames.contains(fileName)) {
			includeFileNames.addAll(
				JSPSourceUtil.getJSPIncludeFileNames(
					fileName, includeFileNames, contentsMap, false));
			includeFileNames.addAll(
				JSPSourceUtil.getJSPReferenceFileNames(
					fileName, includeFileNames, contentsMap,
					".*\\.(jsp|jspf|tag)"));
		}

		checkedForIncludesFileNames.add(fileName);

		String[] includeFileNamesArray = includeFileNames.toArray(
			new String[0]);

		for (String includeFileName : includeFileNamesArray) {
			if (!checkedForUnusedJSPTerm.contains(includeFileName) &&
				_isJSPTermRequired(
					includeFileName, null, regex, type, checkedForUnusedJSPTerm,
					checkedForIncludesFileNames, includeFileNames,
					contentsMap)) {

				return true;
			}
		}

		return false;
	}

	private List<String> _allFileNames;
	private Map<String, String> _contentsMap;

}