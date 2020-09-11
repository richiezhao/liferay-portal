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

package com.liferay.mule.internal.metadata;

import com.fasterxml.jackson.databind.JsonNode;

import com.liferay.mule.internal.connection.LiferayConnection;
import com.liferay.mule.internal.oas.OASFormat;
import com.liferay.mule.internal.oas.OASType;
import com.liferay.mule.internal.oas.constants.OASConstants;
import com.liferay.mule.internal.util.JsonNodeReader;
import com.liferay.mule.internal.util.StringUtil;

import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectFieldTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Matija Petanjek
 */
public class MetadataTypeBuilder {

	public MetadataType buildMetadataType(
			MetadataContext metadataContext, String endpoint, String operation,
			String referencePath)
		throws ConnectionException, MetadataResolvingException {

		JsonNode oasJsonNode = getOASJsonNode(metadataContext);

		JsonNode referenceJsonNode = fetchReferenceJsonNode(
			oasJsonNode, endpoint, operation, referencePath);

		if (referenceJsonNode.isNull()) {
			return resolveAnyMetadataType(metadataContext);
		}

		String schemaName = getSchemaName(referenceJsonNode.textValue());

		JsonNode schemaJsonNode = getSchemaJsonNode(oasJsonNode, schemaName);

		String schemaType = getSchemaType(schemaJsonNode);

		JsonNode propertiesJsonNode = schemaJsonNode.get(
			OASConstants.PROPERTIES);

		if (schemaType.equals(OASConstants.ARRAY)) {
			ArrayTypeBuilder arrayTypeBuilder = getArrayTypeBuilder(
				metadataContext, schemaName);

			resolveArrayMetadataType(
				arrayTypeBuilder, oasJsonNode,
				jsonNodeReader.getDescendantJsonNode(
					propertiesJsonNode, OASConstants.PATH_ITEMS_ITEMS_REF));

			return arrayTypeBuilder.build();
		}

		ObjectTypeBuilder objectTypeBuilder = getObjectTypeBuilder(
			metadataContext, schemaName);

		resolveObjectMetadataType(
			objectTypeBuilder, oasJsonNode, propertiesJsonNode,
			fetchRequiredJsonNode(schemaJsonNode));

		return objectTypeBuilder.build();
	}

	protected ArrayTypeBuilder getArrayTypeBuilder(
		MetadataContext metadataContext, String label) {

		BaseTypeBuilder baseTypeBuilder = metadataContext.getTypeBuilder();

		return baseTypeBuilder.create(
			MetadataFormat.JSON
		).arrayType(
		).label(
			label
		);
	}

	protected JsonNode getOASJsonNode(MetadataContext metadataContext)
		throws ConnectionException, MetadataResolvingException {

		Optional<LiferayConnection> liferayConnectionOptional =
			metadataContext.getConnection();

		try {
			LiferayConnection liferayConnection =
				liferayConnectionOptional.get();

			return jsonNodeReader.fromHttpResponse(
				liferayConnection.getOpenAPISpec());
		}
		catch (IOException ioException) {
			logger.error(
				"Unable to read OpenAPI document from Liferay Portal instance",
				ioException);

			throw new MetadataResolvingException(
				ioException.getMessage(),
				FailureCode.NO_DYNAMIC_METADATA_AVAILABLE, ioException);
		}
		catch (TimeoutException timeoutException) {
			logger.error(
				"Unable to establish connection to Liferay Portal instance",
				timeoutException);

			throw new MetadataResolvingException(
				timeoutException.getMessage(), FailureCode.CONNECTION_FAILURE,
				timeoutException);
		}
	}

	protected ObjectTypeBuilder getObjectTypeBuilder(
		MetadataContext metadataContext, String label) {

		BaseTypeBuilder baseTypeBuilder = metadataContext.getTypeBuilder();

		return baseTypeBuilder.create(
			MetadataFormat.JSON
		).objectType(
		).label(
			label
		);
	}

	protected MetadataType resolveAnyMetadataType(
		MetadataContext metadataContext) {

		BaseTypeBuilder baseTypeBuilder = metadataContext.getTypeBuilder();

		return baseTypeBuilder.anyType(
		).build();
	}

	private JsonNode fetchReferenceJsonNode(
		JsonNode openAPISpecJsonNode, String endpoint, String operation,
		String referencePath) {

		String path = StringUtil.replace(
			referencePath, "ENDPOINT_TPL", endpoint, "OPERATION_TPL",
			operation);

		return jsonNodeReader.fetchDescendantJsonNode(
			openAPISpecJsonNode, path);
	}

	private JsonNode fetchRequiredJsonNode(JsonNode schemaJsonNode) {
		return jsonNodeReader.fetchDescendantJsonNode(
			schemaJsonNode, OASConstants.REQUIRED);
	}

	private MetadataType getMetadataType(JsonNode propertyJsonNode) {
		JsonNode typeJsonNode = propertyJsonNode.get(OASConstants.TYPE);

		OASType oasType = OASType.fromDefinition(typeJsonNode.textValue());

		JsonNode formatJsonNode = propertyJsonNode.get(OASConstants.FORMAT);

		String oasFormatValue = null;

		if (formatJsonNode != null) {
			oasFormatValue = formatJsonNode.textValue();
		}

		OASFormat oasFormat = OASFormat.fromOpenAPITypeAndFormat(
			oasType, oasFormatValue);

		BaseTypeBuilder baseTypeBuilder = BaseTypeBuilder.create(
			MetadataFormat.JSON);

		if (oasFormat == OASFormat.BIGDECIMAL) {
			baseTypeBuilder.numberType();
		}
		else if (oasFormat == OASFormat.BINARY) {
			baseTypeBuilder.binaryType();
		}
		else if (oasFormat == OASFormat.BOOLEAN) {
			baseTypeBuilder.booleanType();
		}
		else if (oasFormat == OASFormat.BYTE) {
			baseTypeBuilder.numberType();
		}
		else if (oasFormat == OASFormat.DATE) {
			baseTypeBuilder.dateType();
		}
		else if (oasFormat == OASFormat.DATE_TIME) {
			baseTypeBuilder.dateTimeType();
		}
		else if (oasFormat == OASFormat.DICTIONARY) {
			baseTypeBuilder.stringType();
		}
		else if (oasFormat == OASFormat.DOUBLE) {
			baseTypeBuilder.numberType();
		}
		else if (oasFormat == OASFormat.FLOAT) {
			baseTypeBuilder.numberType();
		}
		else if (oasFormat == OASFormat.INT32) {
			baseTypeBuilder.numberType();
		}
		else if (oasFormat == OASFormat.INT64) {
			baseTypeBuilder.numberType();
		}
		else if (oasFormat == OASFormat.STRING) {
			baseTypeBuilder.stringType();
		}
		else {
			baseTypeBuilder.anyType();
		}

		return baseTypeBuilder.build();
	}

	private JsonNode getSchemaJsonNode(
		JsonNode openAPISpecJsonNode, String schemaName) {

		String path = StringUtil.replace(
			OASConstants.PATH_COMPONENTS_SCHEMAS_PATTERN, "SCHEMA_TPL",
			schemaName);

		return jsonNodeReader.getDescendantJsonNode(openAPISpecJsonNode, path);
	}

	private String getSchemaName(String reference) {
		return reference.replaceAll(OASConstants.PATH_SCHEMA_REFERENCE, "");
	}

	private String getSchemaType(JsonNode schemaJsonNode) {
		JsonNode typeJsonNode = schemaJsonNode.get("type");

		return typeJsonNode.textValue();
	}

	private void resolveAdditionalPropertiesMetadataType(
		ObjectFieldTypeBuilder objectFieldTypeBuilder) {

		objectFieldTypeBuilder.value(
		).objectType(
		).description(
			"Dictionary"
		);
	}

	private void resolveArrayMetadataType(
		ArrayTypeBuilder arrayTypeBuilder, JsonNode oasJsonNode,
		JsonNode referenceJsonNode) {

		ObjectTypeBuilder objectTypeBuilder = arrayTypeBuilder.of(
		).objectType();

		JsonNode schemaJsonNode = getSchemaJsonNode(
			oasJsonNode, getSchemaName(referenceJsonNode.textValue()));

		resolveObjectMetadataType(
			objectTypeBuilder, oasJsonNode,
			schemaJsonNode.get(OASConstants.PROPERTIES),
			fetchRequiredJsonNode(schemaJsonNode));
	}

	private void resolveNestedArrayMetadataType(
		ObjectFieldTypeBuilder objectFieldTypeBuilder, JsonNode oasJsonNode,
		JsonNode propertyJsonNode) {

		ArrayTypeBuilder nestedArrayTypeBuilder = objectFieldTypeBuilder.value(
		).arrayType();

		if (jsonNodeReader.hasPath(
				propertyJsonNode, OASConstants.PATH_ITEMS_REF)) {

			JsonNode referenceJsonNode = jsonNodeReader.getDescendantJsonNode(
				propertyJsonNode, OASConstants.PATH_ITEMS_REF);

			resolveArrayMetadataType(
				nestedArrayTypeBuilder, oasJsonNode, referenceJsonNode);
		}
		else {
			nestedArrayTypeBuilder.of(
				getMetadataType(propertyJsonNode.get(OASConstants.ITEMS)));
		}
	}

	private void resolveNestedObjectMetadataType(
		ObjectFieldTypeBuilder objectFieldTypeBuilder, JsonNode oasJsonNode,
		JsonNode propertyJsonNode) {

		ObjectTypeBuilder nestedObjectTypeBuilder =
			objectFieldTypeBuilder.value(
			).objectType();

		String schemaName = getSchemaName(
			propertyJsonNode.get(
				OASConstants.REF
			).asText());

		JsonNode nestedObjectSchemaJsonNode = getSchemaJsonNode(
			oasJsonNode, schemaName);

		JsonNode nestedObjectPropertiesJsonNode =
			nestedObjectSchemaJsonNode.get(OASConstants.PROPERTIES);

		JsonNode nestedObjectRequiredJsonNode = fetchRequiredJsonNode(
			nestedObjectSchemaJsonNode);

		resolveObjectMetadataType(
			nestedObjectTypeBuilder, oasJsonNode,
			nestedObjectPropertiesJsonNode, nestedObjectRequiredJsonNode);
	}

	private void resolveObjectMetadataType(
		ObjectTypeBuilder objectTypeBuilder, JsonNode oasJsonNode,
		JsonNode propertiesJsonNode, JsonNode requiredJsonNode) {

		Iterator<Map.Entry<String, JsonNode>> propertiesIterator =
			propertiesJsonNode.fields();

		while (propertiesIterator.hasNext()) {
			Map.Entry<String, JsonNode> propertyEntry =
				propertiesIterator.next();

			ObjectFieldTypeBuilder objectFieldTypeBuilder =
				objectTypeBuilder.addField();

			setObjectFieldKey(objectFieldTypeBuilder, propertyEntry);
			setObjectFieldRequired(
				objectFieldTypeBuilder, propertyEntry.getKey(),
				requiredJsonNode);
			setObjectFieldValue(
				objectFieldTypeBuilder, propertyEntry, oasJsonNode);
		}
	}

	private void setObjectFieldKey(
		ObjectFieldTypeBuilder objectFieldTypeBuilder,
		Map.Entry<String, JsonNode> propertyEntry) {

		objectFieldTypeBuilder.key(propertyEntry.getKey());
	}

	private void setObjectFieldRequired(
		ObjectFieldTypeBuilder objectFieldTypeBuilder, String propertyName,
		JsonNode requiredJsonNode) {

		for (JsonNode propertyNameJsonNode : requiredJsonNode) {
			if (propertyName.equals(propertyNameJsonNode.textValue())) {
				objectFieldTypeBuilder.required(true);

				return;
			}
		}

		objectFieldTypeBuilder.required(false);
	}

	private void setObjectFieldValue(
		ObjectFieldTypeBuilder objectFieldTypeBuilder,
		Map.Entry<String, JsonNode> propertyEntry, JsonNode oasJsonNode) {

		JsonNode propertyJsonNode = propertyEntry.getValue();

		JsonNode typeJsonNode = propertyJsonNode.get(OASConstants.TYPE);

		if (typeJsonNode == null) {
			resolveNestedObjectMetadataType(
				objectFieldTypeBuilder, oasJsonNode, propertyJsonNode);

			return;
		}
		else if (Objects.equals(
					typeJsonNode.textValue(), OASConstants.OBJECT) &&
				 propertyJsonNode.has(OASConstants.ADDITIONAL_PROPERTIES)) {

			resolveAdditionalPropertiesMetadataType(objectFieldTypeBuilder);

			return;
		}
		else if (Objects.equals(typeJsonNode.textValue(), OASConstants.ARRAY)) {
			resolveNestedArrayMetadataType(
				objectFieldTypeBuilder, oasJsonNode, propertyJsonNode);

			return;
		}

		objectFieldTypeBuilder.value(getMetadataType(propertyJsonNode));
	}

	private static final Logger logger = LoggerFactory.getLogger(
		MetadataTypeBuilder.class);

	private final JsonNodeReader jsonNodeReader = new JsonNodeReader();

}