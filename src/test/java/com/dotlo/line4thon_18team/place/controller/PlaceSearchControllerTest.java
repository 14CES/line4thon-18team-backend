package com.dotlo.line4thon_18team.place.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dotlo.line4thon_18team.auth.jwt.JwtProvider;
import com.dotlo.line4thon_18team.global.config.SecurityConfig;
import com.dotlo.line4thon_18team.place.OpeningHours;
import com.dotlo.line4thon_18team.place.OpeningHours.Hours;
import com.dotlo.line4thon_18team.place.OpeningHours.Range;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.PlaceMatcher;
import com.dotlo.line4thon_18team.place.PlaceMatcher.Candidate;
import com.dotlo.line4thon_18team.place.service.SavedPlaceService;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.service.TripService;
import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlaceSearchController.class)
@Import({SecurityConfig.class, JwtProvider.class})
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class PlaceSearchControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean TripService tripService;
    @MockitoBean PlaceMatcher placeMatcher;
    @MockitoBean OpeningHours openingHours;
    @MockitoBean SavedPlaceService savedPlaceService;

    @Test
    void 여행_도시_중심_50km에서_검색() throws Exception {
        Trip trip = mock(Trip.class);
        when(trip.getCity()).thenReturn("오사카");
        when(tripService.findMemberTrip(1L, 7L)).thenReturn(trip);
        when(placeMatcher.searchNear("이치란", 34.6952474, 135.5012079, 50_000)).thenReturn(List.of(
                new Candidate("ChIJt9Y375HmAGARejKVNQwlkbI", "이치란 우메다시바타점", "오사카 기타구", 34.7, 135.5)));

        mvc.perform(get("/api/places/search").param("query", "이치란").param("tripId", "7").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placeId").value("ChIJt9Y375HmAGARejKVNQwlkbI"))
                .andExpect(jsonPath("$[0].name").value("이치란 우메다시바타점"));
    }

    @Test
    void 검색어_없음_400() throws Exception {
        mvc.perform(get("/api/places/search").param("tripId", "7").header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/places/search").param("query", " ").param("tripId", "7").header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verify(placeMatcher, never()).searchNear(anyString(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void 로그인_없음_401() throws Exception {
        mvc.perform(get("/api/places/search").param("query", "이치란").param("tripId", "7"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }

    @Test
    void 근처_추천은_보낸_중심과_탭_유형으로() throws Exception {
        osakaTrip();
        when(placeMatcher.searchNearby(34.6687, 135.5013, PlaceCategory.CAFE)).thenReturn(List.of(
                new Candidate("cafe-1", "HARBS 난바", "오사카 주오구", 34.665, 135.501, PlaceCategory.CAFE)));

        mvc.perform(get("/api/places/nearby").param("tripId", "7").param("lat", "34.6687").param("lng", "135.5013")
                        .param("category", "CAFE").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value("cafe-1"))
                .andExpect(jsonPath("$[0].category").value("CAFE"));
    }

    @Test
    void 근처_추천_기타_유형과_도시_밖_좌표는_400이고_구글을_부르지_않는다() throws Exception {
        osakaTrip();
        mvc.perform(get("/api/places/nearby").param("tripId", "7").param("lat", "34.6687").param("lng", "135.5013")
                        .param("category", "OTHER").header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/places/nearby").param("tripId", "7").param("lat", "35.6764").param("lng", "139.6500")   // 도쿄
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/places/nearby").param("tripId", "7").param("lat", "NaN").param("lng", "135.5013")
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verify(placeMatcher, never()).searchNearby(anyDouble(), anyDouble(), any());
    }

    @Test
    void 영업시간은_여행_도시_시간으로_주고_정보가_없으면_204() throws Exception {
        osakaTrip();
        Map<DayOfWeek, List<Range>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) days.put(d, List.of(new Range(540, 1080)));
        when(openingHours.get("place-1")).thenReturn(new Hours(false, days));

        mvc.perform(get("/api/places/{placeId}/hours", "place-1").param("tripId", "7").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].day").value("MONDAY"))
                .andExpect(jsonPath("$.days[0].hours[0].open").value("09:00"))
                .andExpect(jsonPath("$.days[0].hours[0].close").value("18:00"))
                .andExpect(jsonPath("$.openNow").isBoolean());

        mvc.perform(get("/api/places/{placeId}/hours", "street").param("tripId", "7").header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }

    private void osakaTrip() {
        Trip trip = mock(Trip.class);
        when(trip.getCity()).thenReturn("오사카");
        when(tripService.findMemberTrip(1L, 7L)).thenReturn(trip);
    }
}
