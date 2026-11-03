package com.dotlo.line4thon_18team.preference;

/** 해석 실패 이유 (로그·모니터링용. 사용자 문구는 동일하게 처리해도 된다). */
public enum FallbackReason {
    /** 호출 자체 실패: API 키 없음, 네트워크, 429/5xx가 계속됨 등 */
    LLM_UNAVAILABLE,
    /** 모델이 응답을 거부함 */
    LLM_REFUSED,
    /** 3번 모두 형식·의미 검사를 통과하지 못함 */
    LLM_OUTPUT_INVALID
}
