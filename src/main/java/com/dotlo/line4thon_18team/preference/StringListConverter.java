package com.dotlo.line4thon_18team.preference;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 문자열 목록을 JSON 문자열 한 칸으로 저장한다. 예) ["해산물, 갑각류는 빼 주세요"]
 * 메모에 쉼표가 들어갈 수 있어서 구분자로 잇지 않고 JSON으로 저장한다.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        return MAPPER.writeValueAsString(values == null ? List.of() : values);
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return MAPPER.readValue(json, TYPE);
    }
}
