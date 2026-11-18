package com.dotlo.line4thon_18team.itinerary.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotlo.line4thon_18team.auth.jwt.JwtProvider;
import com.dotlo.line4thon_18team.global.config.SecurityConfig;
import com.dotlo.line4thon_18team.itinerary.ItineraryEditRequestService;
import com.dotlo.line4thon_18team.itinerary.ItineraryGenerationService;
import com.dotlo.line4thon_18team.itinerary.ItineraryPlaceAddService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 일정 생성 요청 본문 (고른 장소). 생성 자체는 ItineraryGenerationServiceTest */
@WebMvcTest(ItineraryAiController.class)
@Import({SecurityConfig.class, JwtProvider.class})
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class ItineraryGenerateApiTest {

    @Autowired MockMvc mvc;
    @MockitoBean ItineraryGenerationService generationService;
    @MockitoBean ItineraryEditRequestService editRequestService;
    @MockitoBean ItineraryPlaceAddService placeAddService;

    @Test
    void 본문이_없으면_저장_장소_전체() throws Exception {
        mvc.perform(post("/api/trips/7/itineraries/generate").header("X-User-Id", "1"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/trips/7/itineraries/generate").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(generationService, org.mockito.Mockito.times(2)).generateAndSave(7L, 1L, null);
    }

    @Test
    void 고른_장소_id를_넘긴다() throws Exception {
        mvc.perform(post("/api/trips/7/itineraries/generate").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"savedPlaceIds\": [12, 15]}"))
                .andExpect(status().isOk());

        verify(generationService).generateAndSave(7L, 1L, List.of(12L, 15L));
    }

    @Test
    void 빈_목록이나_null_id는_400이고_생성하지_않는다() throws Exception {
        mvc.perform(post("/api/trips/7/itineraries/generate").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"savedPlaceIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(post("/api/trips/7/itineraries/generate").header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"savedPlaceIds\": [12, null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(generationService, never()).generateAndSave(anyLong(), anyLong(), any());
    }
}
