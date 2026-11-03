package com.dotlo.line4thon_18team.global.llm.schema;

import com.dotlo.line4thon_18team.preference.ai.PreferenceInterpretationTask;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrictSchemaLinterTest {

    private final StrictSchemaLinter linter = new StrictSchemaLinter();

    @Test
    void 프로젝트의_LLM_스키마는_strict_규칙을_지킨다() {
        JsonNode schema = new SchemaLoader().load(PreferenceInterpretationTask.SCHEMA_LOCATION);

        assertThat(linter.lint(schema)).isEmpty();
    }

    @Test
    void additionalProperties가_없으면_잡아낸다() {
        JsonNode schema = json("{\"type\": \"object\", \"required\": [\"a\"], \"properties\": {\"a\": {\"type\": \"string\"}}}");

        assertThat(linter.lint(schema)).anyMatch(p -> p.contains("additionalProperties=false"));
    }

    @Test
    void 모든_속성이_required에_없으면_잡아낸다() {
        JsonNode schema = json("""
                {"type": "object", "additionalProperties": false, "required": ["a"],
                 "properties": {"a": {"type": "string"}, "b": {"type": "string"}}}
                """);

        assertThat(linter.lint(schema)).anyMatch(p -> p.contains("모든 속성을 required"));
    }

    @Test
    void 지원하지_않는_키워드와_items_없는_배열을_잡아낸다() {
        JsonNode schema = json("""
                {"type": "object", "additionalProperties": false, "required": ["a", "b"],
                 "properties": {"a": {"type": "string", "minLength": 1}, "b": {"type": "array"}}}
                """);

        assertThat(linter.lint(schema))
                .anyMatch(p -> p.contains("지원하지 않는 키워드 'minLength'"))
                .anyMatch(p -> p.contains("array에는 items"));
    }

    @Test
    void 최상위가_object가_아니면_잡아낸다() {
        assertThat(linter.lint(json("{\"type\": \"array\", \"items\": {\"type\": \"string\"}}")))
                .anyMatch(p -> p.contains("최상위 스키마는 type=object"));
    }

    @Test
    void 규칙에_맞지_않는_스키마는_불러올_때_실패한다() {
        assertThatThrownBy(() -> linter.requireValid("bad", json("{\"type\": \"object\"}")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bad");
    }

    @Test
    void 없는_스키마_파일은_바로_실패한다() {
        assertThatThrownBy(() -> new SchemaLoader().load("llm/schemas/none.schema.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    private static JsonNode json(String text) {
        try {
            return new ObjectMapper().readTree(text);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
