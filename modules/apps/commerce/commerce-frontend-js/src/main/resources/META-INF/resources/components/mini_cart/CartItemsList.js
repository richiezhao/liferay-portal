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

import ClayIcon from '@clayui/icon';
import PropTypes from 'prop-types';
import React, {useContext} from 'react';

import Summary from '../summary/Summary';
import CartItem from './CartItem';
import CartItemsListActions from './CartItemsListActions';
import MiniCartContext from './MiniCartContext';
import {summaryDataMapper} from './util/index';

function CartItemsList({items}) {
	const {cartState, isUpdating, spritemap} = useContext(MiniCartContext),
		{summary = {}} = cartState,
		{itemsQuantity = 0} = summary,
		numberOfItems = (items || []).length;

	return (
		<div className={'mini-cart-items-list'}>
			<CartItemsListActions numberOfItems={numberOfItems} />

			{numberOfItems > 0 ? (
				<>
					<div className={'mini-cart-cart-items'}>
						{items.map((item) => (
							<CartItem item={item} key={item.id} />
						))}
					</div>

					{itemsQuantity > 0 && (
						<>
							<Summary
								dataMapper={summaryDataMapper}
								isLoading={isUpdating}
								summaryData={summary}
							/>
						</>
					)}
				</>
			) : (
				<div className="empty-cart">
					<div className="empty-cart-icon mb-3">
						<ClayIcon
							spritemap={spritemap}
							symbol={'shopping-cart'}
						/>
					</div>

					<p className="empty-cart-label">
						{Liferay.Language.get('add-a-product-to-the-cart')}
					</p>
				</div>
			)}
		</div>
	);
}

CartItemsList.propTypes = {
	items: PropTypes.array,
};

export default CartItemsList;
