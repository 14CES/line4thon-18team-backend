package com.dotlo.line4thon_18team.itinerary.ai;

import java.util.List;

/**
 * AI 일정 생성 출력. 필드는 plan-generation.schema.json과 같다.
 *
 * <p>모델은 "어떤 장소를 며칠째 몇 번째로" 까지만 정한다. 시각은 검증기가 이동시간·영업시간으로 계산한다.
 *
 * @param days     일차별 장소 번호 (방문 순서대로)
 * @param excluded 이번 일정에 넣지 못한 장소 번호 (GEN-PLAN-001-E4)
 */
public record PlanGenerationOutput(List<Day> days, List<Integer> excluded) {

    public record Day(int dayNumber, List<Integer> placeNumbers) {

        public Day {
            placeNumbers = placeNumbers == null ? List.of() : List.copyOf(placeNumbers);
        }
    }

    public PlanGenerationOutput {
        days = days == null ? List.of() : List.copyOf(days);
        excluded = excluded == null ? List.of() : List.copyOf(excluded);
    }
}
