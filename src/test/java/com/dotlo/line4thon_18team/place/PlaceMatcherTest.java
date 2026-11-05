package com.dotlo.line4thon_18team.place;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// API 키·DB 없이 도는 테스트만. 실제 API 호출 테스트는 키가 필요해서 CI에서 제외.
class PlaceMatcherTest {

    @Test
    void featureId에서_CID_변환() {
        assertEquals("12867106344743023226", PlaceMatcher.cidOf("0x6000e691ef37d6b7:0xb291250c3595327a"));
        assertNull(PlaceMatcher.cidOf(null));
        assertNull(PlaceMatcher.cidOf("잘못된값"));
    }

    @Test
    void 거리_계산() {
        assertEquals(0, PlaceMatcher.meters(34.6937, 135.5023, 34.6937, 135.5023), 0.001);
        // 공유 URL 좌표 테스트에 쓴 이치란 미도스지점 ↔ 하루카스 300: 약 2.6km
        double d = PlaceMatcher.meters(34.6692878, 135.5006524, 34.6463062, 135.5133091);
        assertTrue(d > 2500 && d < 2900, "거리: " + d);
    }

    @Test
    void 이름이_좌표인지() {
        assertTrue(PlaceMatcher.isCoordinates("34.6463062, 135.5133091"));   // TakeoutParser 가 이름 없는 핀에 넣는 형식
        assertTrue(PlaceMatcher.isCoordinates("-33.86,151.2"));
        assertFalse(PlaceMatcher.isCoordinates("하루카스 300"));
        assertFalse(PlaceMatcher.isCoordinates("7-Eleven 34"));
        assertFalse(PlaceMatcher.isCoordinates(null));
    }

    @Test
    void 이름이_좌표인_핀은_구글을_부르지_않고_확인_필요다() {
        PlaceMatcher matcher = new PlaceMatcher("키 없음");   // 구글을 부르면 키 오류로 예외
        var r = matcher.matchPlaces(List.of(new PlaceMatcher.PlaceInput(1L, "34.6463062, 135.5133091", null, 34.6463062, 135.5133091)),
                34.6937, 135.5023);
        assertEquals(PlaceMatcher.MatchStatus.NEEDS_REVIEW, r.get(0).status());
    }

    // 실제 API 호출. 키가 환경변수에 있을 때만 (CI에서는 건너뜀)
    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_MAPS_API_KEY", matches = ".+")
    void 근처_검색() {
        PlaceMatcher matcher = new PlaceMatcher(System.getenv("GOOGLE_MAPS_API_KEY"));
        double lat = 34.6692878, lng = 135.5006524;  // 이치란 난바 미도스지점
        var cafes = matcher.searchNear("조용한 카페", lat, lng, 500);
        assertFalse(cafes.isEmpty());
        cafes.forEach(c -> assertTrue(PlaceMatcher.meters(lat, lng, c.lat(), c.lng()) <= 500, c.name()));
        assertTrue(matcher.searchNear("ㅁㄴㅇㄹㅁㄴㅇㄹ", lat, lng, 500).isEmpty());
        cafes.forEach(c -> System.out.println("카페: " + c.name() + " / "
                + Math.round(PlaceMatcher.meters(lat, lng, c.lat(), c.lng())) + "m"));
    }
}
