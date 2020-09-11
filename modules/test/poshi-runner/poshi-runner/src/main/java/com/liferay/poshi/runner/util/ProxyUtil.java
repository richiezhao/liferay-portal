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

package com.liferay.poshi.runner.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.proxy.CaptureType;

import org.openqa.selenium.Proxy;

/**
 * @author Kenji Heigel
 */
public class ProxyUtil {

	public static BrowserMobProxy getBrowserMobProxy() {
		return _proxyUtil._getBrowserMobProxy();
	}

	public static Proxy getSeleniumProxy() {
		return _proxyUtil._getSeleniumProxy();
	}

	public static void setBrowserMobProxy(BrowserMobProxy browserMobProxy) {
		_proxyUtil._setBrowserMobProxy(browserMobProxy);
	}

	public static void startBrowserMobProxy() {
		_proxyUtil._startBrowserMobProxy();
	}

	public static void stopBrowserMobProxy() {
		_proxyUtil._stopBrowserMobProxy();
	}

	private BrowserMobProxy _getBrowserMobProxy() {
		if (_browserMobProxy == null) {
			_startBrowserMobProxy();
		}

		return _browserMobProxy;
	}

	private Proxy _getSeleniumProxy() {
		if (_browserMobProxy == null) {
			_startBrowserMobProxy();
		}

		Proxy seleniumProxy = ClientUtil.createSeleniumProxy(_browserMobProxy);

		try {
			InetAddress inetAddress = Inet4Address.getLocalHost();

			String hostIPAddress = inetAddress.getHostAddress();

			seleniumProxy.setHttpProxy(
				hostIPAddress + ":" + _browserMobProxy.getPort());
			seleniumProxy.setSslProxy(
				hostIPAddress + ":" + _browserMobProxy.getPort());
		}
		catch (UnknownHostException unknownHostException) {
			throw new RuntimeException(unknownHostException);
		}

		return seleniumProxy;
	}

	private void _setBrowserMobProxy(BrowserMobProxy browserMobProxy) {
		_browserMobProxy = browserMobProxy;
	}

	private void _startBrowserMobProxy() {
		_browserMobProxy = new BrowserMobProxyServer();

		_browserMobProxy.start(0);

		_browserMobProxy.enableHarCaptureTypes(
			CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
	}

	private void _stopBrowserMobProxy() {
		if (_browserMobProxy != null) {
			_browserMobProxy.stop();
		}

		_browserMobProxy = null;
	}

	private static final ProxyUtil _proxyUtil = new ProxyUtil();

	private BrowserMobProxy _browserMobProxy;

}