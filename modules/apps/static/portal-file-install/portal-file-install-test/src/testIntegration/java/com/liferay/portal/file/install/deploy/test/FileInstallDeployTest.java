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

package com.liferay.portal.file.install.deploy.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.PropsValues;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Dictionary;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;

/**
 * @author Matthew Tambara
 */
@RunWith(Arquillian.class)
public class FileInstallDeployTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() {
		Bundle bundle = FrameworkUtil.getBundle(FileInstallDeployTest.class);

		_bundleContext = bundle.getBundleContext();
	}

	@Test
	public void testConfiguration() throws Exception {
		Path path = Paths.get(
			PropsValues.MODULE_FRAMEWORK_CONFIGS_DIR,
			_CONFIGURATION_PID.concat(".config"));

		try {
			_updateConfiguration(
				() -> {
					StringBundler sb = new StringBundler(5);

					sb.append(_TEST_KEY);
					sb.append(StringPool.EQUAL);
					sb.append(StringPool.QUOTE);
					sb.append(_TEST_VALUE_1);
					sb.append(StringPool.QUOTE);

					String content = sb.toString();

					Files.write(path, content.getBytes());
				});

			Configuration configuration = _configurationAdmin.getConfiguration(
				_CONFIGURATION_PID, StringPool.QUESTION);

			Dictionary<String, Object> properties =
				configuration.getProperties();

			Assert.assertEquals(_TEST_VALUE_1, properties.get(_TEST_KEY));

			_updateConfiguration(
				() -> {
					StringBundler sb = new StringBundler(5);

					sb.append(_TEST_KEY);
					sb.append(StringPool.EQUAL);
					sb.append(StringPool.QUOTE);
					sb.append(_TEST_VALUE_2);
					sb.append(StringPool.QUOTE);

					String content = sb.toString();

					Files.write(path, content.getBytes());

					File file = path.toFile();

					file.setLastModified(file.lastModified() + 1000);
				});

			configuration = _configurationAdmin.getConfiguration(
				_CONFIGURATION_PID, StringPool.QUESTION);

			properties = configuration.getProperties();

			Assert.assertEquals(_TEST_VALUE_2, properties.get(_TEST_KEY));

			_updateConfiguration(() -> Files.delete(path));

			configuration = _configurationAdmin.getConfiguration(
				_CONFIGURATION_PID, StringPool.QUESTION);

			Assert.assertNull(configuration.getProperties());
		}
		finally {
			Files.deleteIfExists(path);
		}
	}

	@Test
	public void testConfigurationSystem() throws Exception {
		Path path = Paths.get(
			PropsValues.MODULE_FRAMEWORK_CONFIGS_DIR,
			_CONFIGURATION_PID.concat(".config"));

		String systemTestPropertyKey = StringBundler.concat(
			_CONFIGURATION_PID, StringPool.PERIOD, _TEST_KEY);

		System.setProperty(systemTestPropertyKey, _TEST_VALUE_1);

		try {
			_updateConfiguration(
				() -> {
					String content = StringBundler.concat(
						_TEST_KEY, StringPool.EQUAL, "\"${",
						systemTestPropertyKey, "}\"");

					Files.write(path, content.getBytes());
				});

			Configuration configuration = _configurationAdmin.getConfiguration(
				_CONFIGURATION_PID, StringPool.QUESTION);

			Dictionary<String, Object> properties =
				configuration.getProperties();

			Assert.assertEquals(_TEST_VALUE_1, properties.get(_TEST_KEY));
		}
		finally {
			System.clearProperty(systemTestPropertyKey);

			Files.deleteIfExists(path);
		}
	}

	@Test
	public void testDeployAndDelete() throws Exception {
		Path path = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR, _TEST_JAR_NAME);

		CountDownLatch installCountDownLatch = new CountDownLatch(1);

		CountDownLatch updateCountDownLatch = new CountDownLatch(3);

		CountDownLatch deleteCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = new BundleListener() {

			@Override
			public void bundleChanged(BundleEvent bundleEvent) {
				Bundle bundle = bundleEvent.getBundle();

				if (!Objects.equals(
						bundle.getSymbolicName(), _TEST_JAR_SYMBOLIC_NAME)) {

					return;
				}

				int type = bundleEvent.getType();

				if (type == BundleEvent.STARTED) {
					installCountDownLatch.countDown();
					updateCountDownLatch.countDown();
				}
				else if (type == BundleEvent.UNINSTALLED) {
					deleteCountDownLatch.countDown();
				}
				else if (type == BundleEvent.UPDATED) {
					updateCountDownLatch.countDown();
				}
			}

		};

		_bundleContext.addBundleListener(bundleListener);

		Version baseVersion = new Version(1, 0, 0);

		Version updateVersion = new Version(2, 0, 0);

		Bundle bundle = null;

		try {
			_createJAR(path, _TEST_JAR_SYMBOLIC_NAME, baseVersion, null);

			installCountDownLatch.await();

			bundle = _getBundle(_TEST_JAR_SYMBOLIC_NAME);

			Assert.assertNotNull(bundle);

			Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
			Assert.assertEquals(baseVersion, bundle.getVersion());

			_createJAR(path, _TEST_JAR_SYMBOLIC_NAME, updateVersion, null);

			updateCountDownLatch.await();

			Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
			Assert.assertEquals(updateVersion, bundle.getVersion());

			Files.delete(path);

			deleteCountDownLatch.await();

			Assert.assertEquals(Bundle.UNINSTALLED, bundle.getState());
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);

			Files.deleteIfExists(path);
		}
	}

	@Test
	public void testDeployAndDeleteFragmentHost() throws Exception {
		String testFragmentSymbolicName = _TEST_JAR_SYMBOLIC_NAME.concat(
			".fragment");

		Path path = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR, _TEST_JAR_NAME);

		Path fragmentPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			testFragmentSymbolicName.concat(".jar"));

		CountDownLatch installCountDownLatch = new CountDownLatch(1);

		CountDownLatch fragmentInstallCountDownLatch = new CountDownLatch(1);

		CountDownLatch deleteCountDownLatch = new CountDownLatch(1);

		CountDownLatch fragmentDeleteCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = new BundleListener() {

			@Override
			public void bundleChanged(BundleEvent bundleEvent) {
				Bundle bundle = bundleEvent.getBundle();

				int type = bundleEvent.getType();

				if (Objects.equals(
						bundle.getSymbolicName(), testFragmentSymbolicName)) {

					if (type == BundleEvent.RESOLVED) {
						fragmentInstallCountDownLatch.countDown();
					}
					else if (type == BundleEvent.UNINSTALLED) {
						fragmentDeleteCountDownLatch.countDown();
					}
				}

				if (Objects.equals(
						bundle.getSymbolicName(), _TEST_JAR_SYMBOLIC_NAME)) {

					if (type == BundleEvent.STARTED) {
						installCountDownLatch.countDown();
					}
					else if (type == BundleEvent.UNINSTALLED) {
						deleteCountDownLatch.countDown();
					}
				}
			}

		};

		_bundleContext.addBundleListener(bundleListener);

		Version version = new Version(1, 0, 0);

		try {
			_createJAR(path, _TEST_JAR_SYMBOLIC_NAME, version, null);

			installCountDownLatch.await();

			Bundle bundle = _getBundle(_TEST_JAR_SYMBOLIC_NAME);

			Assert.assertEquals(Bundle.ACTIVE, bundle.getState());

			_createJAR(
				fragmentPath, testFragmentSymbolicName, version,
				_TEST_JAR_SYMBOLIC_NAME);

			fragmentInstallCountDownLatch.await();

			Bundle fragmentBundle = _getBundle(testFragmentSymbolicName);

			Assert.assertEquals(Bundle.RESOLVED, fragmentBundle.getState());

			Files.delete(path);

			deleteCountDownLatch.await();

			Assert.assertEquals(Bundle.UNINSTALLED, bundle.getState());

			Files.delete(fragmentPath);

			fragmentDeleteCountDownLatch.await();

			Assert.assertEquals(Bundle.UNINSTALLED, fragmentBundle.getState());
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);

			Files.deleteIfExists(path);

			Files.deleteIfExists(fragmentPath);
		}
	}

	private void _createJAR(
			Path path, String symbolicName, Version version,
			String fragmentHost)
		throws IOException {

		try (OutputStream outputStream = Files.newOutputStream(path);
			JarOutputStream jarOutputStream = new JarOutputStream(
				outputStream)) {

			Manifest manifest = new Manifest();

			Attributes attributes = manifest.getMainAttributes();

			attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
			attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
			attributes.putValue(Constants.BUNDLE_VERSION, version.toString());

			if (fragmentHost != null) {
				attributes.putValue(Constants.FRAGMENT_HOST, fragmentHost);
			}

			attributes.putValue("Manifest-Version", "2");

			jarOutputStream.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));

			manifest.write(jarOutputStream);

			jarOutputStream.closeEntry();
		}
	}

	private Bundle _getBundle(String symbolicName) {
		for (Bundle currentBundle : _bundleContext.getBundles()) {
			if (Objects.equals(currentBundle.getSymbolicName(), symbolicName)) {
				return currentBundle;
			}
		}

		return null;
	}

	private void _updateConfiguration(UnsafeRunnable<Exception> runnable)
		throws Exception {

		CountDownLatch countDownLatch = new CountDownLatch(2);

		Dictionary<String, Object> properties = new HashMapDictionary<>();

		properties.put(Constants.SERVICE_PID, _CONFIGURATION_PID);

		ServiceRegistration<ManagedService> serviceRegistration =
			_bundleContext.registerService(
				ManagedService.class, props -> countDownLatch.countDown(),
				properties);

		try {
			runnable.run();

			countDownLatch.await();
		}
		finally {
			serviceRegistration.unregister();
		}
	}

	private static final String _CONFIGURATION_PID =
		FileInstallDeployTest.class.getName() + "Configuration";

	private static final String _TEST_JAR_NAME;

	private static final String _TEST_JAR_SYMBOLIC_NAME;

	private static final String _TEST_KEY = "testKey";

	private static final String _TEST_VALUE_1 = "testValue1";

	private static final String _TEST_VALUE_2 = "testValue2";

	@Inject
	private static ConfigurationAdmin _configurationAdmin;

	static {
		Package pkg = FileInstallDeployTest.class.getPackage();

		_TEST_JAR_SYMBOLIC_NAME = pkg.getName();

		_TEST_JAR_NAME = _TEST_JAR_SYMBOLIC_NAME.concat(".jar");
	}

	private BundleContext _bundleContext;

}