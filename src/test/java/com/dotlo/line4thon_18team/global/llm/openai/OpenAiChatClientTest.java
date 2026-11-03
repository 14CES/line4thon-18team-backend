package com.dotlo.line4thon_18team.global.llm.openai;

import com.dotlo.line4thon_18team.global.llm.LlmCallException;
import com.dotlo.line4thon_18team.global.llm.LlmMessage;
import com.dotlo.line4thon_18team.global.llm.LlmRequest;
import com.dotlo.line4thon_18team.global.llm.LlmResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 로컬 가짜 서버로 OpenAI 요청 형식과 응답·오류 처리를 확인한다 (실제 OpenAI는 호출하지 않음). */
class OpenAiChatClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SUCCESS_BODY = """
            {"id": "chatcmpl-1", "object": "chat.completion",
             "choices": [{"index": 0,
                          "message": {"role": "assistant", "content": "{\\"answer\\": \\"YES\\"}", "refusal": null},
                          "finish_reason": "stop"}]}
            """;

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicInteger hits = new AtomicInteger();
    private volatile int status = 200;
    private volatile String responseBody = SUCCESS_BODY;
    private volatile long delayMillis = 0;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            } catch (IOException ignored) {
                // 클라이언트가 타임아웃으로 먼저 끊은 경우
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void Structured_Outputs_형식으로_요청하고_응답을_읽는다() throws Exception {
        LlmResponse response = client("test-key", null, Duration.ofSeconds(5)).complete(request());

        assertThat(response.content()).isEqualTo("{\"answer\": \"YES\"}");
        assertThat(response.isRefusal()).isFalse();
        assertThat(response.finishReason()).isEqualTo("stop");

        assertThat(path.get()).isEqualTo("/v1/chat/completions");
        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        JsonNode sent = MAPPER.readTree(body.get());
        assertThat(sent.path("model").asString()).isEqualTo("test-model");
        assertThat(sent.path("messages").get(0).path("role").asString()).isEqualTo("system");
        assertThat(sent.path("messages").get(1).path("content").asString()).isEqualTo("질문");
        JsonNode format = sent.path("response_format");
        assertThat(format.path("type").asString()).isEqualTo("json_schema");
        assertThat(format.path("json_schema").path("name").asString()).isEqualTo("answer-task");
        assertThat(format.path("json_schema").path("strict").asBoolean()).isTrue();
        assertThat(format.path("json_schema").path("schema")).isEqualTo(schema());
        assertThat(sent.has("reasoning_effort")).isFalse();
        assertThat(sent.has("temperature")).isFalse();
    }

    @Test
    void reasoning_effort는_설정했을_때만_보낸다() throws Exception {
        client("test-key", "low", Duration.ofSeconds(5)).complete(request());

        assertThat(MAPPER.readTree(body.get()).path("reasoning_effort").asString()).isEqualTo("low");
    }

    @Test
    void 모델_거부를_응답으로_전달한다() {
        responseBody = """
                {"choices": [{"message": {"role": "assistant", "content": null, "refusal": "도울 수 없어요"},
                              "finish_reason": "stop"}]}
                """;

        LlmResponse response = client("test-key", null, Duration.ofSeconds(5)).complete(request());

        assertThat(response.isRefusal()).isTrue();
        assertThat(response.refusal()).isEqualTo("도울 수 없어요");
        assertThat(response.content()).isNull();
    }

    @Test
    void 요청_한도_초과는_재시도_가능한_오류다() {
        status = 429;
        responseBody = "{\"error\": {\"message\": \"Rate limit reached\", \"type\": \"requests\"}}";

        assertThatThrownBy(() -> client("test-key", null, Duration.ofSeconds(5)).complete(request()))
                .isInstanceOfSatisfying(LlmCallException.class, e -> {
                    assertThat(e.isRetryable()).isTrue();
                    assertThat(e.getHttpStatus()).isEqualTo(429);
                    assertThat(e.getMessage()).contains("Rate limit reached");
                });
    }

    @Test
    void 잘못된_요청은_재시도하지_않는다() {
        status = 400;
        responseBody = "{\"error\": {\"message\": \"Invalid schema for response_format\"}}";

        assertThatThrownBy(() -> client("test-key", null, Duration.ofSeconds(5)).complete(request()))
                .isInstanceOfSatisfying(LlmCallException.class, e -> {
                    assertThat(e.isRetryable()).isFalse();
                    assertThat(e.getMessage()).contains("Invalid schema");
                });
    }

    @Test
    void 서버_오류는_재시도_가능하다() {
        status = 503;
        responseBody = "Service Unavailable";

        assertThatThrownBy(() -> client("test-key", null, Duration.ofSeconds(5)).complete(request()))
                .isInstanceOfSatisfying(LlmCallException.class, e -> assertThat(e.isRetryable()).isTrue());
    }

    @Test
    void 응답이_늦으면_재시도_가능한_타임아웃이다() {
        delayMillis = 1500;

        assertThatThrownBy(() -> client("test-key", null, Duration.ofMillis(300)).complete(request()))
                .isInstanceOfSatisfying(LlmCallException.class, e -> {
                    assertThat(e.isRetryable()).isTrue();
                    assertThat(e.getMessage()).contains("시간 초과");
                });
    }

    @Test
    void API_키가_없으면_호출하지_않고_실패한다() {
        assertThatThrownBy(() -> client("  ", null, Duration.ofSeconds(5)).complete(request()))
                .isInstanceOfSatisfying(LlmCallException.class, e -> {
                    assertThat(e.isRetryable()).isFalse();
                    assertThat(e.getMessage()).contains("OPENAI_API_KEY");
                });
        assertThat(hits.get()).isZero();
    }

    @Test
    void 설정_문자열에_API_키를_노출하지_않는다() {
        OpenAiSettings settings = new OpenAiSettings("sk-secret", null, null, null, null, null);

        assertThat(settings.toString()).doesNotContain("sk-secret");
        assertThat(settings.baseUrl()).isEqualTo(OpenAiSettings.DEFAULT_BASE_URL);
        assertThat(settings.model()).isEqualTo(OpenAiSettings.DEFAULT_MODEL);
    }

    @Test
    void 스키마_이름은_허용_문자만_남긴다() {
        assertThat(OpenAiChatClient.schemaName("취향 해석/v1")).isEqualTo("______v1");
        assertThat(OpenAiChatClient.schemaName("preference-interpretation")).isEqualTo("preference-interpretation");
        assertThat(OpenAiChatClient.schemaName("x".repeat(80))).hasSize(64);
    }

    private OpenAiChatClient client(String apiKey, String reasoningEffort, Duration timeout) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/";
        return new OpenAiChatClient(new OpenAiSettings(apiKey, baseUrl, "test-model", reasoningEffort,
                Duration.ofSeconds(2), timeout));
    }

    private static LlmRequest request() {
        return new LlmRequest("answer-task", List.of(LlmMessage.system("규칙"), LlmMessage.user("질문")), schema());
    }

    private static JsonNode schema() {
        return MAPPER.readTree("""
                {"type": "object", "additionalProperties": false, "required": ["answer"],
                 "properties": {"answer": {"type": "string"}}}
                """);
    }
}
