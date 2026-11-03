package com.dotlo.line4thon_18team.preference.domain;

/**
 * 화면용 취향 칩.
 *
 * @param type  칩 종류
 * @param value 값 코드 (예: RELAXED, CAFE). NOTE는 문장 그대로.
 * @param label 기본 표시 문구. FE가 자체 문구를 쓰면 무시해도 된다.
 */
public record PreferenceChip(ChipType type, String value, String label) {
}
