package com.dotlo.line4thon_18team.global.llm;

import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * LLM 작업 하나의 정의 (취향 해석, 일정 생성, 텍스트 수정 등).
 *
 * <p>작업마다 이 인터페이스만 구현하면 공통 모듈이 JSON 형식 강제, 스키마 검사, 재요청을 처리한다.
 *
 * @param <I> 입력 타입
 * @param <O> 출력 타입 (JSON 스키마와 필드가 같은 record)
 */
public interface LlmTask<I, O> {

    /** 작업 이름. 영문·숫자·-·_ 만 쓴다 (OpenAI json_schema 이름 규칙). */
    String name();

    /** 응답이 따라야 할 JSON 스키마. {@code com.dotlo.line4thon_18team.global.llm.schema.StrictSchemaLinter} 규칙을 지켜야 한다. */
    JsonNode schema();

    /** 출력 record 타입. */
    Class<O> outputType();

    /** 입력으로 대화 메시지를 만든다 (보통 system + user). */
    List<LlmMessage> buildMessages(I input);

    /** 스키마 검사를 통과한 결과를 다듬는다 (중복 제거, 공백 정리 등). */
    default O normalize(O output) {
        return output;
    }

    /**
     * 스키마로 표현할 수 없는 의미 규칙 검사. 위반 내용을 돌려주면 그 내용을 붙여 다시 요청한다.
     *
     * @return 위반 메시지 목록. 비어 있으면 통과.
     */
    default List<String> validate(O output) {
        return List.of();
    }

    /**
     * 입력과 견줘야 하는 규칙 검사 (예: 입력에 없는 항목을 만들어 내지 않았는지).
     * 기본은 입력을 보지 않는 {@link #validate(Object)}를 쓴다.
     */
    default List<String> validate(I input, O output) {
        return validate(output);
    }
}
