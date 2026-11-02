package com.dotlo.line4thon_18team.global.llm;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredLlmExecutorTest {

    record Answer(String answer, int count) {
    }

    /** 테스트용 작업: answer는 YES/NO, count는 0 이상. */
    static final class AnswerTask implements LlmTask<String, Answer> {
        private final JsonNode schema;

        AnswerTask() {
            try {
                schema = new ObjectMapper().readTree("""
                        {"type": "object", "additionalProperties": false, "required": ["answer", "count"],
                         "properties": {"answer": {"type": "string", "enum": ["YES", "NO"]},
                                        "count": {"type": "integer"}}}
                        """);
            } catch (JacksonException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String name() {
            return "answer-task";
        }

        @Override
        public JsonNode schema() {
            return schema;
        }

        @Override
        public Class<Answer> outputType() {
            return Answer.class;
        }

        @Override
        public List<LlmMessage> buildMessages(String question) {
            return List.of(LlmMessage.system("규칙"), LlmMessage.user(question));
        }

        @Override
        public List<String> validate(Answer output) {
            return output.count() < 0 ? List.of("count는 0 이상이어야 합니다") : List.of();
        }
    }

    private static final String VALID = "{\"answer\": \"YES\", \"count\": 2}";

    private final FakeLlmClient client = new FakeLlmClient();
    private final List<Long> sleeps = new ArrayList<>();
    private final StructuredLlmExecutor executor = new StructuredLlmExecutor(client, 3, 500, sleeps::add);
    private final AnswerTask task = new AnswerTask();

    @Test
    void 첫_시도에_통과하면_바로_결과를_돌려준다() {
        client.thenReturn(VALID);

        LlmResult<Answer> result = executor.execute(task, "질문");

        assertThat(result.value()).isEqualTo(new Answer("YES", 2));
        assertThat(result.attempts()).isEqualTo(1);
        LlmRequest request = client.requests().get(0);
        assertThat(request.taskName()).isEqualTo("answer-task");
        assertThat(request.schema()).isEqualTo(task.schema());
        assertThat(request.messages()).extracting(LlmMessage::content).containsExactly("규칙", "질문");
    }

    @Test
    void JSON이_깨지면_문제를_알려주고_다시_요청한다() {
        client.thenReturn("이건 JSON이 아님").thenReturn(VALID);

        LlmResult<Answer> result = executor.execute(task, "질문");

        assertThat(result.attempts()).isEqualTo(2);
        List<LlmMessage> retryMessages = client.requests().get(1).messages();
        assertThat(retryMessages).hasSize(4);
        assertThat(retryMessages.get(2)).isEqualTo(LlmMessage.assistant("이건 JSON이 아님"));
        assertThat(retryMessages.get(3).role()).isEqualTo(LlmMessage.Role.USER);
        assertThat(retryMessages.get(3).content()).contains("JSON으로 읽을 수 없습니다");
    }

    @Test
    void 스키마를_어기면_다시_요청한다() {
        client.thenReturn("{\"answer\": \"MAYBE\", \"count\": 1}")
                .thenReturn("{\"answer\": \"NO\"}")
                .thenReturn("{\"answer\": \"NO\", \"count\": 0}");

        LlmResult<Answer> result = executor.execute(task, "질문");

        assertThat(result.value()).isEqualTo(new Answer("NO", 0));
        assertThat(result.attempts()).isEqualTo(3);
        assertThat(client.requests().get(1).messages().get(3).content()).contains("허용되지 않은 값");
        assertThat(client.requests().get(2).messages().get(5).content()).contains("필수 필드 'count'");
    }

    @Test
    void 의미_규칙을_어기면_다시_요청한다() {
        client.thenReturn("{\"answer\": \"YES\", \"count\": -1}").thenReturn(VALID);

        LlmResult<Answer> result = executor.execute(task, "질문");

        assertThat(result.attempts()).isEqualTo(2);
        assertThat(client.requests().get(1).messages().get(3).content()).contains("count는 0 이상이어야 합니다");
    }

    @Test
    void 세_번_모두_검사에_실패하면_출력_오류로_끝난다() {
        client.thenReturn("{}").thenReturn("{}").thenReturn("{}");

        assertThatThrownBy(() -> executor.execute(task, "질문"))
                .isInstanceOf(LlmOutputInvalidException.class)
                .satisfies(e -> assertThat(((LlmOutputInvalidException) e).getAttemptErrors()).hasSize(3));
        assertThat(client.callCount()).isEqualTo(3);
    }

    @Test
    void 모델이_거부하면_재시도하지_않는다() {
        client.thenRespond(new LlmResponse(null, "답할 수 없습니다", "stop"));

        assertThatThrownBy(() -> executor.execute(task, "질문"))
                .isInstanceOf(LlmRefusalException.class)
                .hasMessageContaining("답할 수 없습니다");
        assertThat(client.callCount()).isEqualTo(1);
    }

    @Test
    void 일시_오류는_기다렸다가_다시_시도한다() {
        client.thenThrow(new LlmCallException("answer-task", "HTTP 429", true, 429, null))
                .thenThrow(new LlmCallException("answer-task", "HTTP 503", true, 503, null))
                .thenReturn(VALID);

        LlmResult<Answer> result = executor.execute(task, "질문");

        assertThat(result.attempts()).isEqualTo(3);
        assertThat(sleeps).containsExactly(500L, 1000L);
    }

    @Test
    void 재시도할_수_없는_호출_오류는_바로_던진다() {
        client.thenThrow(new LlmCallException("answer-task", "HTTP 401", false, 401, null));

        assertThatThrownBy(() -> executor.execute(task, "질문"))
                .isInstanceOf(LlmCallException.class)
                .hasMessageContaining("401");
        assertThat(client.callCount()).isEqualTo(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void 호출이_세_번_모두_실패하면_호출_오류로_끝난다() {
        for (int i = 0; i < 3; i++) {
            client.thenThrow(new LlmCallException("answer-task", "timeout", true, null, null));
        }

        assertThatThrownBy(() -> executor.execute(task, "질문"))
                .isInstanceOfSatisfying(LlmCallException.class, e -> {
                    assertThat(e.isRetryable()).isTrue();
                    assertThat(e.getAttemptErrors()).hasSize(3);
                });
        assertThat(sleeps).containsExactly(500L, 1000L);
    }

    @Test
    void 길이_제한으로_잘린_응답은_다시_요청한다() {
        client.thenRespond(new LlmResponse("{\"answer\": \"YE", null, "length")).thenReturn(VALID);

        LlmResult<Answer> result = executor.execute(task, "질문");

        assertThat(result.attempts()).isEqualTo(2);
        assertThat(client.requests().get(1).messages().get(3).content()).contains("잘렸습니다");
    }

    @Test
    void 최대_시도_횟수는_1_이상이어야_한다() {
        assertThatThrownBy(() -> new StructuredLlmExecutor(client, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
