package com.dotlo.line4thon_18team.global.llm.schema;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemaValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode SCHEMA = json("""
            {"type": "object", "additionalProperties": false, "required": ["name", "tags", "score", "memo"],
             "properties": {
               "name": {"type": "string"},
               "tags": {"type": "array", "items": {"type": "string", "enum": ["A", "B"]}},
               "score": {"type": "integer"},
               "memo": {"type": ["string", "null"]}
             }}
            """);

    private final JsonSchemaValidator validator = new JsonSchemaValidator();

    @Test
    void 스키마에_맞으면_오류가_없다() {
        assertThat(validate("{\"name\": \"x\", \"tags\": [\"A\", \"B\"], \"score\": 3, \"memo\": null}")).isEmpty();
        assertThat(validate("{\"name\": \"x\", \"tags\": [], \"score\": 3.0, \"memo\": \"m\"}")).isEmpty();
    }

    @Test
    void 필수_필드가_없으면_알려준다() {
        assertThat(validate("{\"name\": \"x\", \"tags\": [], \"score\": 1}"))
                .containsExactly("$: 필수 필드 'memo'가 없습니다");
    }

    @Test
    void 정의되지_않은_필드를_막는다() {
        assertThat(validate("{\"name\": \"x\", \"tags\": [], \"score\": 1, \"memo\": null, \"extra\": 1}"))
                .containsExactly("$: 정의되지 않은 필드 'extra'가 있습니다");
    }

    @Test
    void 타입과_enum과_배열_항목을_검사한다() {
        List<String> errors = validate("{\"name\": 1, \"tags\": [\"A\", \"C\"], \"score\": 1.5, \"memo\": 3}");

        assertThat(errors).hasSize(4);
        assertThat(errors).anyMatch(e -> e.startsWith("$.name: \"string\" 타입"));
        assertThat(errors).anyMatch(e -> e.startsWith("$.tags[1]: 허용되지 않은 값 \"C\""));
        assertThat(errors).anyMatch(e -> e.startsWith("$.score: \"integer\" 타입"));
        assertThat(errors).anyMatch(e -> e.startsWith("$.memo: [\"string\",\"null\"] 타입"));
    }

    @Test
    void 최상위가_객체가_아니면_실패한다() {
        assertThat(validate("[1, 2]")).hasSize(1);
    }

    private List<String> validate(String instance) {
        return validator.validate(json(instance), SCHEMA);
    }

    private static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
