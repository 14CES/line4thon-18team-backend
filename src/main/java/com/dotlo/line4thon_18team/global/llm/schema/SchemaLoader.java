package com.dotlo.line4thon_18team.global.llm.schema;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** 클래스패스(src/main/resources)의 LLM 응답 스키마를 읽고 strict 규칙을 검사한다. */
public final class SchemaLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private final StrictSchemaLinter linter = new StrictSchemaLinter();

    /**
     * @param classpathLocation 예: "llm/schemas/preference-interpretation.schema.json"
     * @throws IllegalStateException 파일이 없거나 strict 규칙에 맞지 않을 때 (앱 시작 시점에 바로 드러나게)
     */
    public JsonNode load(String classpathLocation) {
        String path = classpathLocation.startsWith("/") ? classpathLocation.substring(1) : classpathLocation;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = SchemaLoader.class.getClassLoader();
        }
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("스키마 파일을 찾을 수 없습니다: " + path);
            }
            JsonNode schema = mapper.readTree(in);
            linter.requireValid(path, schema);
            return schema;
        } catch (JacksonException e) {
            throw new IllegalStateException("스키마 파일이 올바른 JSON이 아닙니다: " + path, e);
        } catch (IOException e) {
            throw new UncheckedIOException("스키마 파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
