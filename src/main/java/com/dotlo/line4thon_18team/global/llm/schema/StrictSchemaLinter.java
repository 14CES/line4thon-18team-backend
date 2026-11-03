package com.dotlo.line4thon_18team.global.llm.schema;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * LLM 응답 스키마 파일이 OpenAI Structured Outputs strict 모드 규칙과
 * {@link JsonSchemaValidator}가 지원하는 범위를 지키는지 검사한다.
 *
 * <ul>
 *   <li>최상위는 type=object</li>
 *   <li>모든 object에 additionalProperties=false, 모든 속성을 required에 나열</li>
 *   <li>array에는 items 필수</li>
 *   <li>지원 키워드만 사용 (minLength, pattern 등은 의미 규칙 검사에서 처리)</li>
 * </ul>
 */
public final class StrictSchemaLinter {

    private static final Set<String> SUPPORTED_KEYWORDS = Set.of(
            "type", "properties", "required", "additionalProperties", "items", "enum", "description");

    /** @return 문제 목록. 비어 있으면 통과. */
    public List<String> lint(JsonNode schema) {
        List<String> problems = new ArrayList<>();
        if (!"object".equals(schema.path("type").asString())) {
            problems.add("$: 최상위 스키마는 type=object 여야 합니다");
        }
        lintNode(schema, "$", problems);
        return problems;
    }

    public void requireValid(String name, JsonNode schema) {
        List<String> problems = lint(schema);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("LLM 스키마 '" + name + "'가 strict 규칙에 맞지 않습니다: " + problems);
        }
    }

    private void lintNode(JsonNode schema, String path, List<String> problems) {
        if (!schema.isObject()) {
            problems.add(path + ": 스키마는 JSON 객체여야 합니다");
            return;
        }
        schema.propertyNames().forEach(keyword -> {
            if (!SUPPORTED_KEYWORDS.contains(keyword)) {
                problems.add(path + ": 지원하지 않는 키워드 '" + keyword + "'");
            }
        });

        JsonNode type = schema.get("type");
        if (type == null) {
            problems.add(path + ": type이 없습니다");
            return;
        }
        Set<String> types = new HashSet<>();
        if (type.isArray()) {
            type.forEach(t -> types.add(t.asString()));
        } else {
            types.add(type.asString());
        }

        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && !enumNode.isArray()) {
            problems.add(path + ": enum은 배열이어야 합니다");
        }

        if (types.contains("object")) {
            lintObject(schema, path, problems);
        }
        if (types.contains("array")) {
            JsonNode items = schema.get("items");
            if (items == null) {
                problems.add(path + ": array에는 items가 있어야 합니다");
            } else {
                lintNode(items, path + "[]", problems);
            }
        }
    }

    private void lintObject(JsonNode schema, String path, List<String> problems) {
        JsonNode properties = schema.get("properties");
        if (properties == null || !properties.isObject()) {
            problems.add(path + ": object에는 properties가 있어야 합니다");
            return;
        }
        JsonNode additional = schema.get("additionalProperties");
        if (additional == null || !additional.isBoolean() || additional.booleanValue()) {
            problems.add(path + ": strict 모드는 additionalProperties=false 가 필요합니다");
        }

        Set<String> names = new TreeSet<>();
        properties.propertyNames().forEach(names::add);
        Set<String> required = new TreeSet<>();
        JsonNode requiredNode = schema.get("required");
        if (requiredNode != null) {
            requiredNode.forEach(r -> required.add(r.asString()));
        }
        if (!required.equals(names)) {
            problems.add(path + ": strict 모드는 모든 속성을 required에 넣어야 합니다 (properties="
                    + names + ", required=" + required + ")");
        }
        properties.properties().forEach(e -> lintNode(e.getValue(), path + "." + e.getKey(), problems));
    }
}
