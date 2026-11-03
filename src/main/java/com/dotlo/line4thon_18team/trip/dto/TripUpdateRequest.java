package com.dotlo.line4thon_18team.trip.dto;

import com.dotlo.line4thon_18team.trip.entity.Companion;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 여행 기본정보 수정 요청. 바꿀 값만 보낸다. (안 보낸 값은 그대로)
 * 예) { "endDate": "2026-11-10" }
 */
public record TripUpdateRequest(

        @Size(min = 1, max = 50, message = "도시 이름은 1~50자여야 합니다.")
        String city,

        LocalDate startDate,

        LocalDate endDate,

        Companion companion,

        Integer companionCount
) {
}
