package com.dotlo.line4thon_18team.itinerary.ai;

import com.dotlo.line4thon_18team.itinerary.ai.PlanGenerationInput.DayCapacity;
import com.dotlo.line4thon_18team.itinerary.ai.PlanGenerationInput.PlaceRef;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 하루에 들어갈 양을 시간으로 가늠하도록 하루 시간과 장소별 체류 시간을 프롬프트에 넣는지. 모르면 아무것도 붙이지 않는다. */
class PlanDayCapacityPromptTest {

    private static final DayCapacity CAPACITY = new DayCapacity(LocalTime.of(9, 30), LocalTime.of(21, 30), 35);

    @Test
    void 하루_쓸_수_있는_시간과_장소별_체류_시간을_붙인다() {
        PlanGenerationInput input = PlanGenerationInput.of("오사카", 2, null, PreferenceProfile.defaults(), List.of(
                new PlaceRef("p1", "유니버설 스튜디오", PlaceCategory.ACTIVITY, null, 180),
                new PlaceRef("p2", "하브스", PlaceCategory.CAFE, "매일 10:00-20:00", 45)), CAPACITY);

        String prompt = PlanGenerationTask.userPrompt(input);

        assertThat(prompt).contains("하루 쓸 수 있는 시간: 09:30~21:30 (720분). 장소 사이 이동·여유는 35분씩 잡는다");
        assertThat(prompt).contains("1. 유니버설 스튜디오 (체험·테마파크) 체류 180분\n");
        assertThat(prompt).contains("2. 하브스 (카페) 체류 45분 영업: 매일 10:00-20:00");
    }

    @Test
    void 시간을_모르면_아무것도_붙이지_않는다() {
        PlanGenerationInput input = PlanGenerationInput.of("오사카", 2, null, PreferenceProfile.defaults(), List.of(
                new PlaceRef("p1", "도톤보리", PlaceCategory.SIGHTSEEING)));

        String prompt = PlanGenerationTask.userPrompt(input);

        assertThat(prompt).doesNotContain("하루 쓸 수 있는 시간").doesNotContain("체류");
    }

    @Test
    void 하루_시간을_모르면_체류_시간도_붙이지_않는다() {
        PlanGenerationInput input = PlanGenerationInput.of("오사카", 2, null, PreferenceProfile.defaults(), List.of(
                new PlaceRef("p1", "도톤보리", PlaceCategory.SIGHTSEEING, null, 90)));

        assertThat(PlanGenerationTask.userPrompt(input)).doesNotContain("체류");
    }

    @Test
    void 다시_배치해도_하루_시간을_유지한다() {
        PlanGenerationInput input = PlanGenerationInput.of("오사카", 2, null, PreferenceProfile.defaults(), List.of(
                new PlaceRef("p1", "도톤보리", PlaceCategory.SIGHTSEEING, null, 90)), CAPACITY)
                .retryWith(List.of("그날 일정이 59분 넘칩니다"));

        assertThat(PlanGenerationTask.userPrompt(input)).contains("하루 쓸 수 있는 시간: 09:30~21:30");
    }

    @Test
    void 시스템_프롬프트가_시간_규칙을_가진다() {
        assertThat(PlanGenerationTask.DAY_BUDGET_RULE).contains("하루 쓸 수 있는 시간").contains("체류");
    }

    @Test
    void 시간_규칙은_하루_시간을_알릴_때만_시스템_프롬프트에_붙는다() {
        PlanGenerationInput small = PlanGenerationInput.of("오사카", 2, null, PreferenceProfile.defaults(), List.of(
                new PlaceRef("p1", "도톤보리", PlaceCategory.SIGHTSEEING, null, 90)));
        PlanGenerationInput crowded = PlanGenerationInput.of("오사카", 2, null, PreferenceProfile.defaults(), List.of(
                new PlaceRef("p1", "도톤보리", PlaceCategory.SIGHTSEEING, null, 90)), CAPACITY);

        assertThat(PlanGenerationTask.systemPrompt(small)).doesNotContain("하루 쓸 수 있는 시간");
        assertThat(PlanGenerationTask.systemPrompt(crowded)).contains("하루 쓸 수 있는 시간");
    }
}
