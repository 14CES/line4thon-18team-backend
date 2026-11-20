package com.dotlo.line4thon_18team.placeai;

import com.dotlo.line4thon_18team.place.PlaceCategory;

/**
 * 장소 AI 답변 입력. 구글 장소 정보 중 답변 근거로 쓸 수 있는 것만 담는다.
 *
 * @param rating      평점. 리뷰가 없으면 null
 * @param reviewCount 리뷰 수. 없으면 null
 * @param summary     구글 한 줄 소개. 없으면 null
 */
public record PlaceAnswerInput(String name, PlaceCategory category, Double rating, Integer reviewCount,
                               String summary, String address, String question) {
}
