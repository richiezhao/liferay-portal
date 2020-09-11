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

package com.liferay.change.tracking.internal.reference;

import com.liferay.change.tracking.spi.reference.TableReferenceDefinition;
import com.liferay.petra.sql.dsl.Column;
import com.liferay.petra.sql.dsl.Table;
import com.liferay.petra.sql.dsl.ast.ASTNode;
import com.liferay.petra.sql.dsl.ast.ASTNodeListener;
import com.liferay.petra.sql.dsl.query.DSLQuery;
import com.liferay.petra.sql.dsl.query.FromStep;
import com.liferay.petra.sql.dsl.query.JoinStep;
import com.liferay.petra.sql.dsl.spi.expression.DefaultPredicate;
import com.liferay.petra.sql.dsl.spi.expression.Operand;
import com.liferay.petra.sql.dsl.spi.query.From;
import com.liferay.petra.sql.dsl.spi.query.Join;
import com.liferay.petra.sql.dsl.spi.query.JoinType;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Preston Crary
 */
public class TableJoinHolderFactory {

	public static <T extends Table<T>> TableJoinHolder create(
		TableReferenceDefinition<T> tableReferenceDefinition,
		Column<T, Long> primaryKeyColumn,
		Function<FromStep, JoinStep> joinFunction) {

		JoinStep joinStep = joinFunction.apply(_validationFromStep);

		if (!(joinStep instanceof Join)) {
			throw new IllegalArgumentException(
				StringBundler.concat("Missing join in \"", joinStep, "\""));
		}

		JoinStepASTNodeListener<T> joinStepASTNodeListener =
			new JoinStepASTNodeListener<>(tableReferenceDefinition.getTable());

		joinStep.toSQL(_emptyStringConsumer, joinStepASTNodeListener);

		if (joinStepASTNodeListener._fromTable == null) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"Join function must use provided from step for join step ",
					"\"", joinStep, "\""));
		}

		if (!joinStepASTNodeListener._hasRequiredTable) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"Required table \"", tableReferenceDefinition.getTable(),
					"\" is unused in join step \"", joinStep, "\""));
		}

		if (joinStepASTNodeListener._invalidJoin != null) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"Invalid join for join step \"", joinStep,
					"\", ensure table alias is used for self joins"));
		}

		if (joinStepASTNodeListener._invalidJoinType != null) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"Invalid join type \"",
					joinStepASTNodeListener._invalidJoinType,
					"\" for join step \"", joinStep, "\""));
		}

		if (joinStepASTNodeListener._invalidOperand != null) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"Invalid predicate operand \"",
					joinStepASTNodeListener._invalidOperand,
					"\" for join step \"", joinStep, "\""));
		}

		if (!joinStepASTNodeListener._tables.containsAll(
				joinStepASTNodeListener._columnTables)) {

			List<Table<?>> columnTables = new ArrayList<>(
				joinStepASTNodeListener._columnTables);

			Comparator<Table<?>> comparator = Comparator.comparing(
				Table::getName);

			columnTables.sort(comparator);

			List<Table<?>> joinTables = new ArrayList<>(
				joinStepASTNodeListener._tables);

			joinTables.sort(comparator);

			throw new IllegalArgumentException(
				StringBundler.concat(
					"Predicate column tables ", columnTables,
					" do not match join tables ", joinTables,
					" for join step \"", joinStep, "\""));
		}

		Column<?, Long> fromPKColumn = TableUtil.getPrimaryKeyColumn(
			joinStepASTNodeListener._fromTable);

		if (fromPKColumn == null) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"No long type primary key column found for table \"",
					joinStepASTNodeListener._fromTable, "\" for join step \"",
					joinStep, "\""));
		}

		Column<T, Long> joinPKColumn =
			joinStepASTNodeListener._aliasPrimaryKeyColumn;

		if (joinPKColumn == null) {
			joinPKColumn = primaryKeyColumn;
		}

		if (fromPKColumn == joinPKColumn) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"From table should be a different table than \"",
					tableReferenceDefinition.getTable(), "\" for join step \"",
					joinStep, "\""));
		}

		return new TableJoinHolder(fromPKColumn, joinPKColumn, joinFunction);
	}

	private static final Consumer<String> _emptyStringConsumer = string -> {
	};
	private static final FromStep _validationFromStep =
		new ValidationFromStep();
	private static final Set<Operand> _validOperands = new HashSet<Operand>() {
		{
			add(Operand.AND);
			add(Operand.EQUAL);
			add(Operand.LIKE);
		}
	};

	private static class JoinStepASTNodeListener<T extends Table<T>>
		implements ASTNodeListener {

		@Override
		public void process(ASTNode astNode) {
			if (astNode instanceof Column) {
				Column<?, ?> column = (Column<?, ?>)astNode;

				_columnTables.add(column.getTable());

				if (!_hasRequiredTable &&
					Objects.equals(_table, column.getTable())) {

					_hasRequiredTable = true;
				}
			}
			else if (astNode instanceof DefaultPredicate) {
				DefaultPredicate defaultPredicate = (DefaultPredicate)astNode;

				Operand operand = defaultPredicate.getOperand();

				if (!_validOperands.contains(operand) &&
					(_invalidOperand == null)) {

					_invalidOperand = operand;
				}
			}
			else if (astNode instanceof From) {
				From from = (From)astNode;

				if (from.getChild() == _validationFromStep) {
					_fromTable = from.getTable();

					_tables.add(from.getTable());
				}
			}
			else if (astNode instanceof Join) {
				Join join = (Join)astNode;

				JoinType joinType = join.getJoinType();

				if (joinType != JoinType.INNER) {
					_invalidJoinType = joinType;
				}

				Table<?> table = join.getTable();

				if (table.equals(_fromTable) &&
					Objects.equals(_fromTable.getName(), table.getName())) {

					_invalidJoin = join;
				}

				if (table.equals(_table) &&
					!Objects.equals(table.getName(), _table.getName())) {

					_aliasPrimaryKeyColumn = TableUtil.getPrimaryKeyColumn(
						(T)table);
				}

				_tables.add(table);
			}
		}

		private JoinStepASTNodeListener(T table) {
			_table = table;
		}

		private Column<T, Long> _aliasPrimaryKeyColumn;
		private Set<Table<?>> _columnTables = Collections.newSetFromMap(
			new IdentityHashMap<>());
		private Table<?> _fromTable;
		private boolean _hasRequiredTable;
		private Join _invalidJoin;
		private JoinType _invalidJoinType;
		private Operand _invalidOperand;
		private final T _table;
		private Set<Table<?>> _tables = Collections.newSetFromMap(
			new IdentityHashMap<>());

	}

	private static class ValidationFromStep implements FromStep {

		@Override
		public Table<?> as(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public JoinStep from(Table<?> table) {
			return new From(this, table);
		}

		@Override
		public void toSQL(
			Consumer<String> consumer, ASTNodeListener astNodeListener) {

			consumer.accept(StringPool.TRIPLE_PERIOD);
		}

		@Override
		public DSLQuery union(DSLQuery dslQuery) {
			throw new UnsupportedOperationException();
		}

		@Override
		public DSLQuery unionAll(DSLQuery dslQuery) {
			throw new UnsupportedOperationException();
		}

	}

}