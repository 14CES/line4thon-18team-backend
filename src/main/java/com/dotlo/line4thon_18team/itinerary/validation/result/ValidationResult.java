package com.dotlo.line4thon_18team.itinerary.validation.result;

import java.util.List;

/** 일정 검증 결과 (GEN-CHECK-001). 계산된 시간표 + 문제 목록 + 요약. */
public record ValidationResult(List<DayTimeline> days, List<ValidationIssue> issues, ValidationSummary summary) {

    public ValidationResult {
        days = days == null ? List.of() : List.copyOf(days);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public static ValidationResult of(List<DayTimeline> days, List<ValidationIssue> issues) {
        int errors = (int) issues.stream().filter(issue -> issue.severity() == Severity.ERROR).count();
        int warnings = issues.size() - errors;
        int placesNeedingCheck = (int) issues.stream()
                .filter(issue -> issue.kind() != IssueKind.ADVISORY && issue.placeId() != null)
                .map(ValidationIssue::placeId)
                .distinct()
                .count();
        return new ValidationResult(days, issues, new ValidationSummary(errors == 0, errors, warnings, placesNeedingCheck));
    }

    public List<ValidationIssue> issuesOf(IssueCode code) {
        return issues.stream().filter(issue -> issue.code() == code).toList();
    }
}
