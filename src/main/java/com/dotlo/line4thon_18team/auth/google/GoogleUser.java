package com.dotlo.line4thon_18team.auth.google;

/**
 * 구글 ID 토큰에서 꺼낸 사용자 정보.
 *
 * @param sub          구글 계정 고유 번호 (사람 구분용, 안 바뀜)
 * @param email        이메일
 * @param name         이름
 * @param picture      프로필 사진 주소
 * @param refreshToken 구글 API를 나중에 다시 부를 때 쓰는 토큰 (저장 장소 가져오기용, 지금은 안 씀)
 */
public record GoogleUser(String sub, String email, String name, String picture, String refreshToken) {
}
