package com.dotlo.line4thon_18team.place;

import static org.junit.jupiter.api.Assertions.*;

import com.dotlo.line4thon_18team.place.PlaceMatcher.LatLng;
import com.dotlo.line4thon_18team.place.TravelTime.Leg;
import com.dotlo.line4thon_18team.place.TravelTime.Mode;
import com.dotlo.line4thon_18team.place.TravelTime.Route;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class TravelTimeTest {

    static final LatLng 미도스지점 = new LatLng(34.6692878, 135.5006524);
    static final LatLng 하루카스 = new LatLng(34.6463062, 135.5133091);
    static final LatLng 글리코사인 = new LatLng(34.6687, 135.5013);

    @Test
    void 시간_변환() {
        assertEquals(52, TravelTime.minutes(new Route("3061s", null)));  // 51분 1초 → 52분
        assertEquals(1, TravelTime.minutes(new Route("60s", null)));
    }

    @Test
    void 직선거리_추정() {
        Leg near = TravelTime.straightLine(미도스지점, 글리코사인);
        assertEquals(new Leg(2, Mode.WALK, true), near);
        Leg far = TravelTime.straightLine(미도스지점, 하루카스);  // 직선 약 2.8km
        assertEquals(Mode.TRANSIT, far.mode());
        assertTrue(far.estimated());
        assertEquals(23, far.minutes());
    }

    // 실제 API 호출. 키가 환경변수에 있을 때만 (CI에서는 건너뜀)
    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_MAPS_API_KEY", matches = ".+")
    void 실제_API() {
        TravelTime service = new TravelTime(System.getenv("GOOGLE_MAPS_API_KEY"));
        Instant noonKst = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(3, 0).toInstant(ZoneOffset.UTC);

        // 오사카 가까운 곳 → 도보
        Leg near = service.get(미도스지점, 글리코사인, noonKst);
        assertEquals(Mode.WALK, near.mode());
        assertFalse(near.estimated());

        // 오사카 먼 곳 → 대중교통 경로 없음 → 자동차 + 10분 추정
        Leg far = service.get(미도스지점, 하루카스, noonKst);
        assertEquals(Mode.TRANSIT, far.mode());
        assertTrue(far.estimated());
        assertTrue(far.minutes() >= 15 && far.minutes() <= 40, "분: " + far.minutes());

        // 서울역 → 강남역 → 실제 대중교통
        Leg seoul = service.get(new LatLng(37.5547, 126.9706), new LatLng(37.4979, 127.0276), noonKst);
        assertEquals(Mode.TRANSIT, seoul.mode());
        assertFalse(seoul.estimated());

        System.out.println("도보: " + near + " / 오사카 추정: " + far + " / 서울: " + seoul);
    }
}
