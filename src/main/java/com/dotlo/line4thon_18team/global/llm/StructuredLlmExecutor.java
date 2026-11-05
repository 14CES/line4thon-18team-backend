package com.dotlo.line4thon_18team.global.llm;

import com.dotlo.line4thon_18team.global.llm.schema.JsonSchemaValidator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * LLM 연동 공통 모듈.
 *
 * <p>한 번의 {@link #execute} 호출에서 다음을 처리한다.
 * <ol>
 *   <li>작업의 JSON 스키마를 붙여 모델 호출 (OpenAI Structured Outputs로 형식 강제)</li>
 *   <li>응답을 JSON으로 읽고 같은 스키마로 다시 검사</li>
 *   <li>작업별 의미 규칙 검사 ({@link LlmTask#validate})</li>
 *   <li>실패하면 문제 내용을 대화에 붙여 다시 요청. 최대 {@code maxAttempts}번 (기본 3번)</li>
 * </ol>
 * 429·5xx·타임아웃 같은 일시 오류도 시도 횟수에 포함하고, 잠깐 기다렸다가 다시 시도한다.
 * 잘못된 요청(400)·인증 실패(401)·모델 거부는 바로 실패로 끝낸다.
 */
public class StructuredLlmExecutor {

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_BASE_BACKOFF_MILLIS = 500L;
    private static final Logger log = LoggerFactory.getLogger(StructuredLlmExecutor.class);

    /** 재시도 대기 (테스트에서 실제로 기다리지 않도록 바꿔 끼운다). */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final LlmClient client;
    private final int maxAttempts;
    private final long baseBackoffMillis;
    private final Sleeper sleeper;
    private final ObjectMapper mapper = strictMapper();
    private final JsonSchemaValidator schemaValidator = new JsonSchemaValidator();

    public StructuredLlmExecutor(LlmClient client, int maxAttempts) {
        this(client, maxAttempts, DEFAULT_BASE_BACKOFF_MILLIS, Thread::sleep);
    }

    public StructuredLlmExecutor(LlmClient client, int maxAttempts, long baseBackoffMillis, Sleeper sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts는 1 이상이어야 합니다: " + maxAttempts);
        }
        this.client = Objects.requireNonNull(client, "client");
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = Math.max(0L, baseBackoffMillis);
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    /**
     * 작업을 실행하고 검사를 통과한 결과를 돌려준다.
     *
     * @throws LlmCallException          재시도할 수 없는 호출 오류, 또는 모든 시도가 호출 단계에서 실패한 경우
     * @throws LlmRefusalException       모델이 응답을 거부한 경우
     * @throws LlmOutputInvalidException 모든 시도의 출력이 검사를 통과하지 못한 경우
     */
    public <I, O> LlmResult<O> execute(LlmTask<I, O> task, I input) {
        List<LlmMessage> conversation = new ArrayList<>(task.buildMessages(input));
        List<String> attemptErrors = new ArrayList<>();
        LlmCallException lastCallFailure = null;
        boolean receivedOutput = false;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            LlmResponse response;
            try {
                response = client.complete(new LlmRequest(task.name(), conversation, task.schema()));
            } catch (LlmCallException e) {
                if (!e.isRetryable()) {
                    throw e;
                }
                lastCallFailure = e;
                attemptErrors.add(attempt + "회차 호출 실패: " + e.getMessage());
                log.warn("LLM task={} attempt={}/{} call failed: {}", task.name(), attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    backoff(task.name(), attempt);
                }
                continue;
            }

            if (response.isRefusal()) {
                throw new LlmRefusalException(task.name(), response.refusal(), attemptErrors);
            }
            receivedOutput = true;

            List<String> problems = new ArrayList<>();
            O value = response.isTruncated()
                    ? truncated(problems)
                    : parseAndCheck(task, input, response.content(), problems);

            if (problems.isEmpty()) {
                if (attempt > 1) {
                    log.info("LLM task={} succeeded on attempt {}", task.name(), attempt);
                }
                return new LlmResult<>(value, attempt);
            }

            attemptErrors.add(attempt + "회차 출력 오류: " + String.join("; ", problems));
            log.warn("LLM task={} attempt={}/{} invalid output: {}", task.name(), attempt, maxAttempts, problems);
            conversation.add(LlmMessage.assistant(response.content()));
            conversation.add(LlmMessage.user(repairInstruction(problems)));
        }

        if (!receivedOutput && lastCallFailure != null) {
            throw new LlmCallException(task.name(),
                    "LLM 호출이 " + maxAttempts + "번 모두 실패했습니다: " + lastCallFailure.getMessage(),
                    true, lastCallFailure.getHttpStatus(), lastCallFailure, attemptErrors);
        }
        throw new LlmOutputInvalidException(task.name(), attemptErrors);
    }

    private <I, O> O parseAndCheck(LlmTask<I, O> task, I input, String content, List<String> problems) {
        if (content == null || content.isBlank()) {
            problems.add("응답이 비어 있습니다");
            return null;
        }
        JsonNode node;
        try {
            node = mapper.readTree(content);
        } catch (JacksonException e) {
            problems.add("JSON으로 읽을 수 없습니다: " + e.getOriginalMessage());
            return null;
        }
        List<String> schemaErrors = schemaValidator.validate(node, task.schema());
        if (!schemaErrors.isEmpty()) {
            problems.addAll(schemaErrors);
            return null;
        }
        O value;
        try {
            value = mapper.treeToValue(node, task.outputType());
        } catch (JacksonException e) {
            problems.add("결과 객체로 바꿀 수 없습니다: " + e.getOriginalMessage());
            return null;
        }
        value = task.normalize(value);
        problems.addAll(task.validate(input, value));
        return value;
    }

    private static <O> O truncated(List<String> problems) {
        problems.add("응답이 길이 제한에 걸려 잘렸습니다. 더 짧게 답하세요");
        return null;
    }

    static String repairInstruction(List<String> problems) {
        return "직전 응답이 규칙을 지키지 않았습니다.\n문제: " + String.join(" / ", problems)
                + "\n같은 JSON 스키마를 지켜서 처음부터 다시 답하세요. JSON 외의 설명은 쓰지 마세요.";
    }

    private void backoff(String taskName, int attempt) {
        long millis = baseBackoffMillis * (1L << (attempt - 1));
        if (millis <= 0) {
            return;
        }
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmCallException(taskName, "재시도 대기 중 인터럽트되었습니다", false, null, e);
        }
    }

    /** LLM 출력 전용 매퍼 (스프링 공용 JsonMapper와 별개로, 모르는 필드·null 숫자·뒤에 붙은 글자를 모두 거부). */
    static ObjectMapper strictMapper() {
        return JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
    }
}
