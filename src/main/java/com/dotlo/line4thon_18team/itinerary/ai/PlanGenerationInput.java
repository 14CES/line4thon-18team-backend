package com.dotlo.line4thon_18team.itinerary.ai;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;

import java.util.List;
import java.util.Objects;

/**
 * AI 일정 생성 입력 (GEN-PLAN-001).
 *
 * <p>장소는 번호를 붙여서 넘긴다. place ID는 길고 비슷해서 모델이 그대로 옮겨 적다가 틀리기 쉬운데,
 * 번호로 받으면 입력에 없는 장소를 만들어 내는 것도 바로 걸러낼 수 있다.
 *
 * @param dayCount 여행 일수 (1일차 ~ dayCount일차)
 */
public record PlanGenerationInput(String city, int dayCount, PreferenceProfile preferences, List<Candidate> places) {

    /** @param number 1부터 시작하는 장소 번호. 모델은 이 번호로만 답한다. */
    public record Candidate(int number, String placeId, String name, PlaceCategory category) {
    }

    public PlanGenerationInput {
        Objects.requireNonNull(preferences, "preferences");
        if (dayCount < 1) {
            throw new IllegalArgumentException("여행 일수는 1일 이상이어야 합니다: " + dayCount);
        }
        places = places == null ? List.of() : List.copyOf(places);
        if (places.isEmpty()) {
            throw new IllegalArgumentException("배치할 장소가 없습니다");
        }
    }

    /** 장소 목록에 번호를 붙여 만든다. */
    public static PlanGenerationInput of(String city, int dayCount, PreferenceProfile preferences,
                                         List<PlaceRef> places) {
        List<Candidate> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            PlaceRef place = places.get(i);
            candidates.add(new Candidate(i + 1, place.placeId(), place.name(), place.category()));
        }
        return new PlanGenerationInput(city, dayCount, preferences, candidates);
    }

    /** 번호를 붙이기 전의 장소. */
    public record PlaceRef(String placeId, String name, PlaceCategory category) {
    }

    public Candidate byNumber(int number) {
        return places.get(number - 1);
    }

    public boolean hasNumber(int number) {
        return number >= 1 && number <= places.size();
    }
}
