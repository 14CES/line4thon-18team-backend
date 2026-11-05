package com.dotlo.line4thon_18team.place.dto;

import java.util.List;

/**
 * 장소 가져오기 결과 (Takeout 업로드, 공유 리스트 링크). 리스트별로 몇 개가 새로 들어갔고 몇 개가 중복/건너뜀인지 알려준다.
 */
public record PlaceImportResponse(Long tripId, int totalNewCount, List<ListResult> lists) {

    /**
     * @param placeCount     이 리스트에서 읽은 장소 수
     * @param newCount       새로 저장된 장소 수
     * @param duplicateCount 이미 저장돼 있던 장소 수 (리스트 이름만 추가됨)
     * @param skippedCount   장소를 알아낼 수 없어서 건너뛴 행 수
     */
    public record ListResult(String listName, int placeCount, int newCount, int duplicateCount, int skippedCount) {
    }
}
