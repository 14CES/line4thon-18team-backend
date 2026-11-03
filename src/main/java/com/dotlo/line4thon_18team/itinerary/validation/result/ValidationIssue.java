package com.dotlo.line4thon_18team.itinerary.validation.result;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 검증에서 찾은 문제 하나.
 *
 * @param date    문제가 있는 날짜
 * @param itemId  문제가 있는 일정 항목 (하루 전체 문제면 null)
 * @param placeId 문제가 있는 장소 (하루 전체 문제면 null)
 * @param params  문구를 만들 때 쓸 값 (시각은 "HH:mm" 문자열)
 */
public record ValidationIssue(IssueCode code,
                              Severity severity,
                              IssueKind kind,
                              LocalDate date,
                              String itemId,
                              String placeId,
                              Map<String, Object> params) {

    public ValidationIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(date, "date");
        params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
