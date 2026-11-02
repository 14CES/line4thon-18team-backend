package com.dotlo.line4thon_18team.itinerary.validation.result;

/**
 * 검증 요약.
 *
 * @param valid              ERROR가 없으면 true (AI 생성 일정은 false면 재배치·재생성)
 * @param placesNeedingCheck 일정 충돌·정보 누락 문제가 있는 장소 수 (확정 시 "확인이 필요한 장소 n곳"용, 기준은 BE1과 합의 필요)
 */
public record ValidationSummary(boolean valid, int errorCount, int warningCount, int placesNeedingCheck) {
}
