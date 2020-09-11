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

package com.liferay.portal.db.partition;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.dao.jdbc.util.DataSourceWrapper;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.CurrentConnectionUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.InfrastructureUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.spring.hibernate.DialectDetector;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

/**
 * @author Alberto Chaparro
 */
public class DBPartitionUtil {

	public static boolean addDBPartition(long companyId)
		throws PortalException {

		if (!_DATABASE_PARTITION_ENABLED || (companyId == _defaultCompanyId)) {
			return false;
		}

		Connection connection = CurrentConnectionUtil.getConnection(
			InfrastructureUtil.getDataSource());

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"create schema if not exists " + _getSchemaName(companyId) +
					" character set utf8")) {

			preparedStatement.executeUpdate();

			DatabaseMetaData databaseMetaData = connection.getMetaData();

			DBInspector dbInspector = new DBInspector(connection);

			try (ResultSet resultSet = databaseMetaData.getTables(
					dbInspector.getCatalog(), dbInspector.getSchema(), null,
					new String[] {"TABLE"});
				Statement statement = connection.createStatement()) {

				while (resultSet.next()) {
					String tableName = resultSet.getString("TABLE_NAME");

					if (_isControlTable(dbInspector, tableName)) {
						statement.executeUpdate(
							StringBundler.concat(
								"create view ", _getSchemaName(companyId),
								StringPool.PERIOD, tableName, " as select * ",
								"from ", _defaultSchemaName, StringPool.PERIOD,
								tableName));
					}
					else {
						statement.executeUpdate(
							StringBundler.concat(
								"create table ", _getSchemaName(companyId),
								StringPool.PERIOD, tableName, " like ",
								_defaultSchemaName, StringPool.PERIOD,
								tableName));
					}
				}
			}

			_useSchema(connection);
		}
		catch (Exception exception) {
			throw new PortalException(exception);
		}

		return true;
	}

	public static boolean removeDBPartition(long companyId) {
		return _DATABASE_PARTITION_ENABLED;
	}

	public static void setDefaultCompanyId(long companyId) {
		if (_DATABASE_PARTITION_ENABLED) {
			_defaultCompanyId = companyId;
		}
	}

	public static DataSource wrapDataSource(DataSource dataSource)
		throws SQLException {

		if (!_DATABASE_PARTITION_ENABLED) {
			return dataSource;
		}

		DB db = DBManagerUtil.getDB(
			DBManagerUtil.getDBType(DialectDetector.getDialect(dataSource)),
			dataSource);

		if (db.getDBType() != DBType.MYSQL) {
			throw new Error("Database partition requires MySQL");
		}

		try (Connection connection = dataSource.getConnection()) {
			_defaultSchemaName = connection.getCatalog();
		}

		return new DataSourceWrapper(dataSource) {

			@Override
			public Connection getConnection() throws SQLException {
				Connection connection = super.getConnection();

				_useSchema(connection);

				return connection;
			}

			@Override
			public Connection getConnection(String userName, String password)
				throws SQLException {

				Connection connection = super.getConnection(userName, password);

				_useSchema(connection);

				return connection;
			}

		};
	}

	private static String _getSchemaName(long companyId) {
		return _DATABASE_PARTITION_SCHEMA_NAME_PREFIX + companyId;
	}

	private static boolean _isControlTable(
			DBInspector dbInspector, String tableName)
		throws Exception {

		if (_controlTableNames.contains(tableName) ||
			tableName.startsWith("QUARTZ_") ||
			!dbInspector.hasColumn(tableName, "companyId")) {

			return true;
		}

		return false;
	}

	private static void _useSchema(Connection connection) throws SQLException {
		if (connection.isReadOnly()) {
			return;
		}

		long companyId = CompanyThreadLocal.getCompanyId();

		try (Statement statement = connection.createStatement()) {
			if ((companyId == CompanyConstants.SYSTEM) ||
				(companyId == _defaultCompanyId)) {

				statement.execute("use " + _defaultSchemaName);
			}
			else {
				statement.execute("use " + _getSchemaName(companyId));
			}
		}
	}

	private static final boolean _DATABASE_PARTITION_ENABLED =
		GetterUtil.getBoolean(PropsUtil.get("database.partition.enabled"));

	private static final String _DATABASE_PARTITION_SCHEMA_NAME_PREFIX =
		GetterUtil.get(
			PropsUtil.get("database.partition.schema.name.prefix"),
			"lpartition_");

	private static final Set<String> _controlTableNames = new HashSet<>(
		Arrays.asList("Company", "Portlet", "VirtualHost"));
	private static volatile long _defaultCompanyId;
	private static String _defaultSchemaName;

}