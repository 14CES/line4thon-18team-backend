package com.dotlo.line4thon_18team.place;

import static org.junit.jupiter.api.Assertions.*;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.place.PlaceDetails.Detail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// 실제 API 호출. 키가 환경변수에 있을 때만 (CI에서는 건너뜀). 값은 2026-11-17 확인
@EnabledIfEnvironmentVariable(named = "GOOGLE_MAPS_API_KEY", matches = ".+")
class PlaceDetailsTest {

    private final PlaceDetails service = new PlaceDetails(System.getenv("GOOGLE_MAPS_API_KEY"));

    @Test
    void 평점_리뷰_수_한국어_소개() {
        Detail d = service.get("ChIJr_gZonkIAWARB1xyACZNUKM");  // 산넨자카
        assertEquals("산넨자카", d.name());
        assertEquals(PlaceCategory.SIGHTSEEING, d.category());
        assertTrue(d.rating() > 4.0);
        assertTrue(d.reviewCount() > 10_000);
        assertTrue(d.summary().contains("거리"));
        assertTrue(d.address().contains("Kyoto"));
        assertEquals(35.0, d.lat(), 0.05);
    }

    @Test
    void 소개가_없는_장소는_null() {
        Detail d = service.get("ChIJQ1WVyW7nAGARYEXVTLi4XHw");  // 하브스 난바파크스점
        assertEquals(PlaceCategory.CAFE, d.category());
        assertNull(d.summary());
    }

    @Test
    void 없는_장소_ID는_INVALID_PLACE_ID() {
        BusinessException e = assertThrows(BusinessException.class, () -> service.get("ChIJ_없는_장소"));
        assertEquals(ErrorCode.INVALID_PLACE_ID, e.getErrorCode());
    }
}
