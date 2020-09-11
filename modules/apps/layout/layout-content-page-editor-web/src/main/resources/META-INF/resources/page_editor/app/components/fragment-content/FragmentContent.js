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

import classNames from 'classnames';
import {useIsMounted} from 'frontend-js-react-web';
import PropTypes from 'prop-types';
import React, {useCallback, useEffect, useState} from 'react';

import setFragmentEditables from '../../actions/setFragmentEditables';
import selectCanConfigureWidgets from '../../selectors/selectCanConfigureWidgets';
import selectSegmentsExperienceId from '../../selectors/selectSegmentsExperienceId';
import {useDispatch, useSelector, useSelectorCallback} from '../../store/index';
import {getFrontendTokenValue} from '../../utils/getFrontendTokenValue';
import {getResponsiveConfig} from '../../utils/getResponsiveConfig';
import loadBackgroundImage from '../../utils/loadBackgroundImage';
import {
	useGetContent,
	useGetFieldValue,
	useToControlsId,
} from '../CollectionItemContext';
import {useGlobalContext} from '../GlobalContext';
import UnsafeHTML from '../UnsafeHTML';
import {useIsProcessorEnabled} from './EditableProcessorContext';
import getAllEditables from './getAllEditables';
import resolveEditableValue from './resolveEditableValue';

const FragmentContent = ({
	className,
	elementRef,
	fragmentEntryLinkId,
	getPortals,
	item,
	withinTopper = false,
}) => {
	const dispatch = useDispatch();
	const isMounted = useIsMounted();
	const isProcessorEnabled = useIsProcessorEnabled();
	const globalContext = useGlobalContext();
	const toControlsId = useToControlsId();
	const getFieldValue = useGetFieldValue();

	const canConfigureWidgets = useSelector(selectCanConfigureWidgets);

	/**
	 * Updates editables array for the rendered fragment.
	 * @param {HTMLElement} [nextFragmentElement] Fragment element
	 *  If not specified, fragmentElement state is used instead.
	 * @return {Array} Updated editables array
	 */
	const onRender = useCallback(
		(fragmentElement) => {
			let updatedEditableValues = [];

			if (isMounted()) {
				updatedEditableValues = getAllEditables(fragmentElement);
			}

			dispatch(
				setFragmentEditables(
					fragmentEntryLinkId,
					toControlsId(item.itemId),
					updatedEditableValues
				)
			);

			return updatedEditableValues;
		},
		[dispatch, fragmentEntryLinkId, isMounted, item, toControlsId]
	);

	const fragmentEntryLink = useSelectorCallback(
		(state) => state.fragmentEntryLinks[fragmentEntryLinkId],
		[fragmentEntryLinkId]
	);

	const languageId = useSelector((state) => state.languageId);
	const segmentsExperienceId = useSelector(selectSegmentsExperienceId);
	const selectedViewportSize = useSelector(
		(state) => state.selectedViewportSize
	);

	const defaultContent = useGetContent(
		fragmentEntryLink,
		segmentsExperienceId
	);
	const [content, setContent] = useState(defaultContent);
	const editableValues = fragmentEntryLink
		? fragmentEntryLink.editableValues
		: {};

	const fragmentEntryLinkError = fragmentEntryLink?.error;

	useEffect(() => {
		if (fragmentEntryLinkError) {
			throw new Error(fragmentEntryLinkError);
		}
	}, [fragmentEntryLinkError]);

	/**
	 * fragmentElement keeps a copy of the fragment real HTML,
	 * we perform editableValues replacements over this copy
	 * to avoid multiple re-renders, when every replacement has
	 * finished, this function must be called.
	 *
	 * Synchronizes fragmentElement's content to the real fragment
	 * content. When this happens, the real re-render is performed.
	 */
	useEffect(() => {
		let fragmentElement = document.createElement('div');

		if (!isProcessorEnabled()) {
			fragmentElement.innerHTML = defaultContent;

			Promise.all(
				getAllEditables(fragmentElement).map((editable) =>
					resolveEditableValue(
						editableValues,
						editable.editableId,
						editable.editableValueNamespace,
						languageId,
						getFieldValue
					).then(([value, editableConfig]) => {
						editable.processor.render(
							editable.element,
							value,
							editableConfig
						);

						editable.element.classList.add('page-editor__editable');
					})
				)
			).then(() => {
				if (isMounted() && fragmentElement) {
					setContent(fragmentElement.innerHTML);
				}
			});
		}

		return () => {
			fragmentElement = null;
		};
	}, [
		defaultContent,
		editableValues,
		fragmentEntryLinkId,
		getFieldValue,
		isMounted,
		isProcessorEnabled,
		languageId,
	]);

	const responsiveConfig = getResponsiveConfig(
		item.config,
		selectedViewportSize
	);

	const {
		backgroundColor,
		backgroundImage,
		borderColor,
		borderRadius,
		borderWidth,
		fontFamily,
		fontSize,
		fontWeight,
		height,
		marginBottom,
		marginLeft,
		marginRight,
		marginTop,
		maxHeight,
		maxWidth,
		minHeight,
		minWidth,
		opacity,
		overflow,
		paddingBottom,
		paddingLeft,
		paddingRight,
		paddingTop,
		shadow,
		textAlign,
		textColor,
		width,
	} = responsiveConfig.styles;

	const [backgroundImageValue, setBackgroundImageValue] = useState('');

	useEffect(() => {
		loadBackgroundImage(backgroundImage).then(setBackgroundImageValue);
	}, [backgroundImage]);

	const style = {};

	style.backgroundColor = getFrontendTokenValue(backgroundColor);
	style.border = `solid ${borderWidth}px`;
	style.borderColor = getFrontendTokenValue(borderColor);
	style.borderRadius = getFrontendTokenValue(borderRadius);
	style.boxShadow = getFrontendTokenValue(shadow);
	style.color = getFrontendTokenValue(textColor);
	style.fontFamily = getFrontendTokenValue(fontFamily);
	style.fontSize = getFrontendTokenValue(fontSize);
	style.fontWeight = getFrontendTokenValue(fontWeight);
	style.height = height;
	style.maxHeight = maxHeight;
	style.maxWidth = maxWidth;
	style.minHeight = minHeight;
	style.minWidth = minWidth;
	style.opacity = opacity ? opacity / 100 : null;
	style.overflow = overflow;
	style.width = width;

	if (backgroundImageValue) {
		style.backgroundImage = `url(${backgroundImageValue})`;
		style.backgroundPosition = '50% 50%';
		style.backgroundRepeat = 'no-repeat';
		style.backgroundSize = 'cover';
	}

	return (
		<UnsafeHTML
			className={classNames(
				className,
				`mb-${marginBottom || 0}`,
				`ml-${marginLeft || 0}`,
				`mr-${marginRight || 0}`,
				`mt-${marginTop || 0}`,
				`pb-${paddingBottom || 0}`,
				`pl-${paddingLeft || 0}`,
				`pr-${paddingRight || 0}`,
				`pt-${paddingTop || 0}`,
				'page-editor__fragment-content',
				{
					'page-editor__fragment-content--portlet-topper-hidden': !canConfigureWidgets,
					[textAlign
						? textAlign.startsWith('text-')
							? textAlign
							: `text-${textAlign}`
						: '']: textAlign,
				}
			)}
			contentRef={elementRef}
			getPortals={getPortals}
			globalContext={globalContext}
			markup={content}
			onRender={withinTopper ? onRender : () => {}}
			style={style}
		/>
	);
};

FragmentContent.propTypes = {
	className: PropTypes.string,
	fragmentEntryLinkId: PropTypes.string.isRequired,
	getPortals: PropTypes.func.isRequired,
	item: PropTypes.object.isRequired,
	withinTopper: PropTypes.bool,
};

export default React.memo(FragmentContent);
