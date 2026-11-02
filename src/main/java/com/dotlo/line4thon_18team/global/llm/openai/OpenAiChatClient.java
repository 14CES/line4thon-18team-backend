package com.dotlo.line4thon_18team.global.llm.openai;

import com.dotlo.line4thon_18team.global.llm.LlmCallException;
import com.dotlo.line4thon_18team.global.llm.LlmClient;
import com.dotlo.line4thon_18team.global.llm.LlmMessage;
import com.dotlo.line4thon_18team.global.llm.LlmRequest;
import com.dotlo.line4thon_18team.global.llm.LlmResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * OpenAI Chat Completions + Structured Outputs 연동.
 *
 * <p>요청에 {@code response_format: {type: "json_schema", json_schema: {name, strict: true, schema}}}를 넣어
 * 모델이 스키마에 맞는 JSON만 내도록 강제한다. 재시도는 하지 않는다 ({@link com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor} 담당).
 *
 * <p>Spring에 묶이지 않도록 JDK HttpClient를 쓴다.
 */
public class OpenAiChatClient implements LlmClient {

    private final OpenAiSettings settings;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiChatClient(OpenAiSettings settings) {
        this(settings, HttpClient.newBuilder().connectTimeout(settings.connectTimeout()).build());
    }

    public OpenAiChatClient(OpenAiSettings settings, HttpClient httpClient) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (!settings.hasApiKey()) {
            throw new LlmCallException(request.taskName(),
                    "OpenAI API 키가 없습니다. OPENAI_API_KEY 환경변수를 설정하세요", false, null, null);
        }
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(settings.baseUrl() + "/chat/completions"))
                .timeout(settings.requestTimeout())
                .header("Authorization", "Bearer " + settings.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(request), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new LlmCallException(request.taskName(), "OpenAI 응답 시간 초과", true, null, e);
        } catch (IOException e) {
            throw new LlmCallException(request.taskName(), "OpenAI 연결 실패: " + e.getMessage(), true, null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmCallException(request.taskName(), "OpenAI 호출 중 인터럽트되었습니다", false, null, e);
        }

        int status = response.statusCode();
        if (status >= 400) {
            boolean retryable = status == 408 || status == 409 || status == 429 || status >= 500;
            throw new LlmCallException(request.taskName(),
                    "OpenAI HTTP " + status + ": " + errorMessage(response.body()), retryable, status, null);
        }
        return parse(request.taskName(), response.body());
    }

    /** Chat Completions 요청 본문. */
    String requestBody(LlmRequest request) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", settings.model());
        ArrayNode messages = root.putArray("messages");
        for (LlmMessage message : request.messages()) {
            messages.addObject()
                    .put("role", message.role().apiValue())
                    .put("content", message.content());
        }
        ObjectNode format = root.putObject("response_format");
        format.put("type", "json_schema");
        ObjectNode jsonSchema = format.putObject("json_schema");
        jsonSchema.put("name", schemaName(request.taskName()));
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", request.schema());
        if (settings.reasoningEffort() != null) {
            root.put("reasoning_effort", settings.reasoningEffort());
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (JacksonException e) {
            throw new IllegalStateException("OpenAI 요청 본문을 만들 수 없습니다", e);
        }
    }

    /** json_schema.name 규칙: 영문·숫자·_·- 만, 최대 64자. */
    static String schemaName(String taskName) {
        String cleaned = taskName == null ? "" : taskName.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (cleaned.isEmpty()) {
            cleaned = "output";
        }
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    private LlmResponse parse(String taskName, String body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (JacksonException e) {
            throw new LlmCallException(taskName, "OpenAI 응답을 JSON으로 읽을 수 없습니다", true, null, e);
        }
        JsonNode choice = root.path("choices").path(0);
        if (choice.isMissingNode()) {
            throw new LlmCallException(taskName, "OpenAI 응답에 choices가 없습니다", true, null, null);
        }
        JsonNode message = choice.path("message");
        return new LlmResponse(textOrNull(message.get("content")), textOrNull(message.get("refusal")),
                textOrNull(choice.get("finish_reason")));
    }

    private String errorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "(본문 없음)";
        }
        try {
            JsonNode message = mapper.readTree(body).path("error").path("message");
            if (message.isString()) {
                return message.asString();
            }
        } catch (JacksonException ignored) {
            // JSON이 아니면 원문 일부를 쓴다
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.asString();
    }
}
