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

package com.liferay.project.templates.service.builder;

import com.liferay.maven.executor.MavenExecutor;
import com.liferay.project.templates.BaseProjectTemplatesTestCase;
import com.liferay.project.templates.extensions.util.Validator;
import com.liferay.project.templates.util.FileTestUtil;

import java.io.File;

import java.net.URI;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Gregory Amerson
 */
public class ProjectTemplatesServiceBuilderTest
	implements BaseProjectTemplatesTestCase {

	@ClassRule
	public static final MavenExecutor mavenExecutor = new MavenExecutor();

	@BeforeClass
	public static void setUpClass() throws Exception {
		String gradleDistribution = System.getProperty("gradle.distribution");

		if (Validator.isNull(gradleDistribution)) {
			Properties properties = FileTestUtil.readProperties(
				"gradle-wrapper/gradle/wrapper/gradle-wrapper.properties");

			gradleDistribution = properties.getProperty("distributionUrl");
		}

		Assert.assertTrue(gradleDistribution.contains(GRADLE_WRAPPER_VERSION));

		_gradleDistribution = URI.create(gradleDistribution);
	}

	@Test
	public void testBuildTemplateContentDTDVersionServiceBuilder70()
		throws Exception {

		File gradleProjectDir = _buildTemplateWithGradle(
			"service-builder", "foo-bar", "--liferay-version", "7.0.6");

		testContains(
			gradleProjectDir, "foo-bar-service/service.xml",
			"liferay-service-builder_7_0_0.dtd");
	}

	@Test
	public void testBuildTemplateContentDTDVersionServiceBuilder71()
		throws Exception {

		File gradleProjectDir = _buildTemplateWithGradle(
			"service-builder", "foo-bar", "--liferay-version", "7.1.3");

		testContains(
			gradleProjectDir, "foo-bar-service/service.xml",
			"liferay-service-builder_7_1_0.dtd");
	}

	@Test
	public void testBuildTemplateContentDTDVersionServiceBuilder72()
		throws Exception {

		File gradleProjectDir = _buildTemplateWithGradle(
			"service-builder", "foo-bar", "--liferay-version", "7.2.1");

		testContains(
			gradleProjectDir, "foo-bar-service/service.xml",
			"liferay-service-builder_7_2_0.dtd");
	}

	@Test
	public void testBuildTemplateServiceBuilderCheckExports() throws Exception {
		String liferayVersion = getDefaultLiferayVersion();
		String name = "guestbook";
		String packageName = "com.liferay.docs.guestbook";
		String template = "service-builder";

		File gradleWorkspaceDir = buildWorkspace(
			temporaryFolder, "gradle", "gradleWS", liferayVersion,
			mavenExecutor);

		File gradleWorkspaceModulesDir = new File(
			gradleWorkspaceDir, "modules");

		File gradleProjectDir = buildTemplateWithGradle(
			gradleWorkspaceModulesDir, template, name, "--package-name",
			packageName, "--liferay-version", liferayVersion);

		File gradleServiceXml = new File(
			new File(gradleProjectDir, name + "-service"), "service.xml");

		Consumer<Document> consumer = document -> {
			Element documentElement = document.getDocumentElement();

			documentElement.setAttribute("package-path", "com.liferay.test");
		};

		editXml(gradleServiceXml, consumer);

		File mavenWorkspaceDir = buildWorkspace(
			temporaryFolder, "maven", "mavenWS", liferayVersion, mavenExecutor);

		File mavenModulesDir = new File(mavenWorkspaceDir, "modules");

		File mavenProjectDir = buildTemplateWithMaven(
			mavenModulesDir, mavenModulesDir, template, name, "com.test",
			mavenExecutor, "-Dpackage=" + packageName,
			"-DliferayVersion=" + liferayVersion);

		File mavenServiceXml = new File(
			new File(mavenProjectDir, name + "-service"), "service.xml");

		editXml(mavenServiceXml, consumer);

		testContains(
			gradleProjectDir, name + "-api/bnd.bnd", "Export-Package:\\",
			packageName + ".exception,\\", packageName + ".model,\\",
			packageName + ".service,\\", packageName + ".service.persistence");

		if (isBuildProjects()) {
			Optional<String> stdOutput = executeGradle(
				gradleWorkspaceDir, false, true, _gradleDistribution,
				":modules:" + name + ":" + name + "-service" +
					GRADLE_TASK_PATH_BUILD);

			Assert.assertTrue(stdOutput.isPresent());

			String gradleOutput = stdOutput.get();

			Assert.assertTrue(
				"Expected gradle output to include build error. " +
					gradleOutput,
				gradleOutput.contains("Exporting an empty package"));

			String mavenOutput = executeMaven(
				mavenProjectDir, true, mavenExecutor, MAVEN_GOAL_PACKAGE);

			Assert.assertTrue(
				"Expected maven output to include build error. " + mavenOutput,
				mavenOutput.contains("Exporting an empty package"));
		}
	}

	@Test
	public void testCompareServiceBuilderPluginVersions() throws Exception {
		String liferayVersion = getDefaultLiferayVersion();
		String name = "sample";
		String packageName = "com.test.sample";
		String serviceProjectName = name + "-service";
		String template = "service-builder";

		File gradleWorkspaceDir = buildWorkspace(
			temporaryFolder, "gradle", "gradleWS", liferayVersion,
			mavenExecutor);

		File gradleWorkspaceModulesDir = new File(
			gradleWorkspaceDir, "modules");

		buildTemplateWithGradle(
			gradleWorkspaceModulesDir, template, name, "--package-name",
			packageName, "--liferay-version", liferayVersion);

		Optional<String> gradleResult = executeGradle(
			gradleWorkspaceDir, true, _gradleDistribution,
			":modules:" + name + ":" + serviceProjectName + ":dependencies");

		String gradleServiceBuilderVersion = null;

		Matcher matcher = _serviceBuilderVersionPattern.matcher(
			gradleResult.get());

		if (matcher.matches()) {
			gradleServiceBuilderVersion = matcher.group(1);
		}

		File mavenWorkspaceDir = buildWorkspace(
			temporaryFolder, "maven", "mavenWS", liferayVersion, mavenExecutor);

		File mavenModulesDir = new File(mavenWorkspaceDir, "modules");

		File mavenProjectDir = buildTemplateWithMaven(
			mavenModulesDir, mavenModulesDir, template, name, "com.test",
			mavenExecutor, "-Dpackage=" + packageName);

		String mavenResult = executeMaven(
			new File(mavenProjectDir, serviceProjectName), mavenExecutor,
			MAVEN_GOAL_BUILD_SERVICE);

		matcher = _serviceBuilderVersionPattern.matcher(mavenResult);

		String mavenServiceBuilderVersion = null;

		if (matcher.matches()) {
			mavenServiceBuilderVersion = matcher.group(1);
		}

		Assert.assertEquals(
			"com.liferay.portal.tools.service.builder versions do not match",
			gradleServiceBuilderVersion, mavenServiceBuilderVersion);
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File _buildTemplateWithGradle(
			String template, String name, String... args)
		throws Exception {

		File destinationDir = temporaryFolder.newFolder("gradle");

		return buildTemplateWithGradle(destinationDir, template, name, args);
	}

	private static URI _gradleDistribution;
	private static final Pattern _serviceBuilderVersionPattern =
		Pattern.compile(
			".*service\\.builder:([0-9]+\\.[0-9]+\\.[0-9]+).*",
			Pattern.DOTALL | Pattern.MULTILINE);

}