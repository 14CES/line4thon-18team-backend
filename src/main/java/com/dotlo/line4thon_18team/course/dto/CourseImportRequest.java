package com.dotlo.line4thon_18team.course.dto;

import com.dotlo.line4thon_18team.trip.entity.Companion;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 인기 코스 가져오기 요청. 종료일은 코스 기간으로 정해진다.
 * 예) { "startDate": "2026-12-10" }
 *
 * @param companion      생략하면 혼자. 나중에 여행 수정으로 바꾼다
 * @param companionCount 생략할 수 있다 (여행 생성과 같은 규칙)
 */
public record CourseImportRequest(
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        Companion companion,

        Integer companionCount
) {
}
