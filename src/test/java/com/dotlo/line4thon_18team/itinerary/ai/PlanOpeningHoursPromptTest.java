package com.dotlo.line4thon_18team.itinerary.ai;

import com.dotlo.line4thon_18team.itinerary.ai.PlanEditInput.CurrentDay;
import com.dotlo.line4thon_18team.itinerary.ai.PlanEditInput.CurrentVisit;
import com.dotlo.line4thon_18team.itinerary.ai.PlanGenerationInput.PlaceRef;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 영업시간이 AI 프롬프트에 들어가는지. 모르는 장소는 아무것도 붙이지 않는다. */
class PlanOpeningHoursPromptTest {

    @Test
    void 생성_프롬프트에_장소별_영업시간을_붙인다() {
        PlanGenerationInput input = PlanGenerationInput.of("오사카", 2, null, PreferenceProfile.defaults(), List.of(
                new PlaceRef("p1", "스미요시 대사", PlaceCategory.CULTURE, "매일 06:30-17:00"),
                new PlaceRef("p2", "하브스", PlaceCategory.CAFE)));

        String prompt = PlanGenerationTask.userPrompt(input);

        assertThat(prompt).contains("1. 스미요시 대사 (문화·역사) 영업: 매일 06:30-17:00");
        assertThat(prompt).contains("2. 하브스 (카페)\n");
    }

    @Test
    void 수정_프롬프트에_지금_일정과_추가할_장소의_영업시간을_붙인다() {
        PlanEditInput input = PlanEditInput.of("오사카", 2, PreferenceProfile.defaults(),
                List.of(CurrentDay.of(1, new CurrentVisit("v-1", "p1", "스미요시 대사", PlaceCategory.CULTURE,
                        "1일차 06:30-17:00 / 2일차 휴무"))),
                List.of(new PlaceRef("p2", "오사카성", PlaceCategory.CULTURE, "매일 09:00-17:00")),
                "오사카성 넣어 주세요");

        String prompt = PlanEditTask.userPrompt(input);

        assertThat(prompt).contains("v-1 스미요시 대사 (문화·역사) 영업: 1일차 06:30-17:00 / 2일차 휴무");
        assertThat(prompt).contains("1. 오사카성 (문화·역사) 영업: 매일 09:00-17:00");
    }

    @Test
    void 두_작업_모두_영업시간_규칙을_가진다() {
        assertThat(PlanGenerationTask.SYSTEM_PROMPT).contains("영업:");
        assertThat(PlanEditTask.SYSTEM_PROMPT).contains("영업:");
    }
}
