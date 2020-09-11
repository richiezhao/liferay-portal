/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import {Align} from 'metal-position';
import PropTypes from 'prop-types';
import React from 'react';

import Keywords from './Keywords';
import TotalCount from './TotalCount';
export default function Detail({
	currentPage,
	languageTag,
	onCurrentPageChange,
	onTrafficSourceNameChange,
	trafficShareDataProvider,
	trafficVolumeDataProvider,
}) {
	return (
		<>
			<div className="c-pt-3 c-px-3 d-flex">
				<ClayButton
					displayType="unstyled"
					onClick={() => {
						onCurrentPageChange({view: 'main'});
						onTrafficSourceNameChange('');
					}}
					small={true}
				>
					<ClayIcon symbol="angle-left-small" />
				</ClayButton>

				<div className="align-self-center flex-grow-1 mx-2">
					<strong>{currentPage.data.title}</strong>
				</div>
			</div>

			<div className="c-p-3 traffic-source-detail">
				<TotalCount
					className="mb-2"
					dataProvider={trafficVolumeDataProvider}
					label={Liferay.Util.sub(
						Liferay.Language.get('traffic-volume')
					)}
					languageTag={languageTag}
					popoverAlign={Align.Bottom}
					popoverHeader={Liferay.Language.get('traffic-volume')}
					popoverMessage={Liferay.Language.get(
						'traffic-volume-is-the-estimated-number-of-visitors-coming-to-your-page'
					)}
					popoverPosition="bottom"
				/>

				<TotalCount
					className="mb-4"
					dataProvider={trafficShareDataProvider}
					label={Liferay.Util.sub(
						Liferay.Language.get('traffic-share')
					)}
					percentage={true}
					popoverHeader={Liferay.Language.get('traffic-share')}
					popoverMessage={Liferay.Language.get(
						'traffic-share-is-the-percentage-of-traffic-sent-to-your-page-by-one-source'
					)}
				/>

				<Keywords currentPage={currentPage} languageTag={languageTag} />
			</div>
		</>
	);
}

Detail.proptypes = {
	currentPage: PropTypes.object.isRequired,
	languageTag: PropTypes.string.isRequired,
	onCurrentPageChange: PropTypes.func.isRequired,
	onTrafficSourceNameChange: PropTypes.func.isRequired,
	trafficShareDataProvider: PropTypes.func.isRequired,
	trafficVolumeDataProvider: PropTypes.func.isRequired,
};
