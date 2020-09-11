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

package com.liferay.change.tracking.spi.reference;

import com.liferay.change.tracking.spi.reference.builder.ChildTableReferenceInfoBuilder;
import com.liferay.change.tracking.spi.reference.builder.ParentTableReferenceInfoBuilder;
import com.liferay.petra.sql.dsl.Table;
import com.liferay.portal.kernel.service.persistence.BasePersistence;

/**
 * @author Preston Crary
 */
public interface TableReferenceDefinition<T extends Table<T>> {

	public void defineChildTableReferences(
		ChildTableReferenceInfoBuilder<T> childTableReferenceInfoBuilder);

	public void defineParentTableReferences(
		ParentTableReferenceInfoBuilder<T> parentTableReferenceInfoBuilder);

	public BasePersistence<?> getBasePersistence();

	public T getTable();

}