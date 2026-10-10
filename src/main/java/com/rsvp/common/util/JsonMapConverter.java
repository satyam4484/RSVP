package com.rsvp.common.util;

import java.util.Collections;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();
	private static final TypeReference<Map<String, Object>> TYPE = new TypeReference<>() {
	};

	@Override
	public String convertToDatabaseColumn(Map<String, Object> attribute) {
		if (attribute == null || attribute.isEmpty()) {
			return null;
		}
		return MAPPER.writeValueAsString(attribute);
	}

	@Override
	public Map<String, Object> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return Collections.emptyMap();
		}
		return MAPPER.readValue(dbData, TYPE);
	}
}
