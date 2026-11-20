package com.dotlo.line4thon_18team.placeai;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.llm.FakeLlmClient;
import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.global.llm.schema.SchemaLoader;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.PlaceDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 장소 AI 답변의 프롬프트·출력 검사·서비스 흐름. */
class PlaceAnswerTaskTest {

    private final PlaceAnswerTask task = new PlaceAnswerTask(new SchemaLoader());
    private final FakeLlmClient llm = new FakeLlmClient();

    private static PlaceDetails.Detail detail(Double rating, Integer reviewCount, String summary) {
        return new PlaceDetails.Detail("p1", "오사카성", PlaceCategory.CULTURE, rating, reviewCount, summary,
                "1-1 Osakajo, Chuo Ward", 34.68, 135.52, "https://maps.example/p1");
    }

    private PlaceAnswerService service(PlaceDetails.Detail detail) {
        PlaceDetails details = new PlaceDetails("test-key") {
            @Override
            public Detail get(String placeId) {
                return detail;
            }
        };
        return new PlaceAnswerService(details, new StructuredLlmExecutor(llm, 3, 0, millis -> { }), task);
    }

    @Test
    void 장소_정보와_질문을_프롬프트에_담는다() {
        String prompt = PlaceAnswerTask.userPrompt(new PlaceAnswerInput("오사카성", PlaceCategory.CULTURE, 4.5, 1200,
                "일본의 대표 성", "Chuo Ward", "어떤 곳이에요?"));

        assertThat(prompt).contains("이름: 오사카성").contains("유형: 문화·역사")
                .contains("평점: 4.5 (리뷰 1200개)").contains("소개: 일본의 대표 성")
                .contains("질문: 어떤 곳이에요?");
    }

    @Test
    void 없는_정보는_프롬프트에_넣지_않는다() {
        String prompt = PlaceAnswerTask.userPrompt(new PlaceAnswerInput("하브스", PlaceCategory.CAFE, null, null,
                null, null, "분위기 어때요?"));

        assertThat(prompt).doesNotContain("평점").doesNotContain("소개").doesNotContain("주소");
    }

    @Test
    void 빈_답변과_너무_긴_답변은_다시_요청한다() {
        assertThat(task.validate(new PlaceAnswerOutput("  "))).isNotEmpty();
        assertThat(task.validate(new PlaceAnswerOutput("가".repeat(601)))).isNotEmpty();
        assertThat(task.validate(new PlaceAnswerOutput("괜찮은 곳이에요."))).isEmpty();
    }

    @Test
    void 질문에_답한다() {
        llm.thenReturn("{\"answer\": \"오사카의 대표 성이에요.\"}");

        String answer = service(detail(4.5, 1200, "대표 성")).ask("p1", "  어떤 곳이에요?  ");

        assertThat(answer).isEqualTo("오사카의 대표 성이에요.");
        assertThat(llm.requests().get(0).messages().toString()).contains("질문: 어떤 곳이에요?");
    }

    @Test
    void 빈_질문과_긴_질문은_LLM을_부르지_않고_거절한다() {
        PlaceAnswerService service = service(detail(null, null, null));

        assertThatThrownBy(() -> service.ask("p1", "   ")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.ask("p1", "가".repeat(201))).isInstanceOf(BusinessException.class);
        assertThat(llm.callCount()).isZero();
    }
}
