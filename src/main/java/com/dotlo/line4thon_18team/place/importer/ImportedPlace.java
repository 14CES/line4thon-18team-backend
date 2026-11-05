package com.dotlo.line4thon_18team.place.importer;

/**
 * Takeout 파일이나 공유 리스트 링크에서 읽은 장소 1개 (DB 저장 전).
 * featureId 가 없는 장소(직접 찍은 핀 등)는 좌표가 있다.
 */
public record ImportedPlace(
        String name,
        String note,
        String googleUrl,
        String featureId,
        Double latitude,
        Double longitude
) {
}
