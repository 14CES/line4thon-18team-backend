package com.dotlo.line4thon_18team.global.llm.schema;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * LLM 출력이 JSON 스키마를 지키는지 검사한다.
 *
 * <p>OpenAI strict 모드에서 쓰는 키워드만 지원한다:
 * type(단일/배열), properties, required, additionalProperties(false), items, enum, minItems, maxItems.
 * 스키마 파일은 {@link StrictSchemaLinter}로 이 범위를 벗어나지 않는지 미리 검사한다.
 */
public final class JsonSchemaValidator {

    /** @return 위반 메시지 목록. 비어 있으면 통과. */
    public List<String> validate(JsonNode instance, JsonNode schema) {
        List<String> errors = new ArrayList<>();
        check(instance, schema, "$", errors);
        return errors;
    }

    private void check(JsonNode node, JsonNode schema, String path, List<String> errors) {
        JsonNode type = schema.get("type");
        if (type != null && !matchesType(node, type)) {
            errors.add(path + ": " + type + " 타입이어야 합니다 (받은 값: " + describe(node) + ")");
            return;
        }
        JsonNode allowed = schema.get("enum");
        if (allowed != null && !contains(allowed, node)) {
            errors.add(path + ": 허용되지 않은 값 " + node + " (허용: " + allowed + ")");
        }
        if (node.isObject()) {
            checkObject(node, schema, path, errors);
        } else if (node.isArray()) {
            checkArray(node, schema, path, errors);
        }
    }

    private void checkObject(JsonNode node, JsonNode schema, String path, List<String> errors) {
        JsonNode required = schema.get("required");
        if (required != null) {
            for (JsonNode name : required) {
                if (!node.has(name.asString())) {
                    errors.add(path + ": 필수 필드 '" + name.asString() + "'가 없습니다");
                }
            }
        }
        JsonNode properties = schema.path("properties");
        JsonNode additional = schema.get("additionalProperties");
        boolean closed = additional != null && additional.isBoolean() && !additional.booleanValue();

        Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode propertySchema = properties.get(field.getKey());
            if (propertySchema != null) {
                check(field.getValue(), propertySchema, path + "." + field.getKey(), errors);
            } else if (closed) {
                errors.add(path + ": 정의되지 않은 필드 '" + field.getKey() + "'가 있습니다");
            }
        }
    }

    private void checkArray(JsonNode node, JsonNode schema, String path, List<String> errors) {
        JsonNode minItems = schema.get("minItems");
        if (minItems != null && node.size() < minItems.asInt()) {
            errors.add(path + ": 항목이 최소 " + minItems.asInt() + "개 필요합니다 (받은 개수: " + node.size() + ")");
        }
        JsonNode maxItems = schema.get("maxItems");
        if (maxItems != null && node.size() > maxItems.asInt()) {
            errors.add(path + ": 항목은 최대 " + maxItems.asInt() + "개입니다 (받은 개수: " + node.size() + ")");
        }
        JsonNode items = schema.get("items");
        if (items != null) {
            for (int i = 0; i < node.size(); i++) {
                check(node.get(i), items, path + "[" + i + "]", errors);
            }
        }
    }

    private static boolean matchesType(JsonNode node, JsonNode type) {
        if (type.isArray()) {
            for (JsonNode candidate : type) {
                if (matchesSingleType(node, candidate.asString())) {
                    return true;
                }
            }
            return false;
        }
        return matchesSingleType(node, type.asString());
    }

    private static boolean matchesSingleType(JsonNode node, String type) {
        switch (type) {
            case "object":
                return node.isObject();
            case "array":
                return node.isArray();
            case "string":
                return node.isString();
            case "boolean":
                return node.isBoolean();
            case "null":
                return node.isNull();
            case "number":
                return node.isNumber();
            case "integer":
                return node.isIntegralNumber()
                        || (node.isNumber() && node.decimalValue().stripTrailingZeros().scale() <= 0);
            default:
                return false;
        }
    }

    private static boolean contains(JsonNode allowed, JsonNode node) {
        for (JsonNode candidate : allowed) {
            if (candidate.equals(node)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(JsonNode node) {
        String type = node.getNodeType().name().toLowerCase();
        String text = node.toString();
        return text.length() > 40 ? type : type + " " + text;
    }
}
