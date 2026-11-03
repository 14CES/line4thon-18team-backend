package com.dotlo.line4thon_18team.preference.web;

import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 취향 API의 여행 권한 확인. MySQL이 필요하다 (docker compose up -d).
 * 토큰 대신 X-User-Id 헤더를 쓰려고 auth.dev-header-enabled 를 켠다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "auth.dev-header-enabled=true")
@Transactional
class PreferenceControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;

    private Long ownerId;
    private Long otherId;
    private Long tripId;

    @BeforeEach
    void setUp() {
        User owner = users.save(User.builder().googleSub("sub-owner").email("owner@test.com").name("주인").build());
        User other = users.save(User.builder().googleSub("sub-other").email("other@test.com").name("남").build());
        Trip trip = trips.save(Trip.create(owner, "오사카",
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), Companion.ALONE, null));
        ownerId = owner.getId();
        otherId = other.getId();
        tripId = trip.getId();
    }

    @Test
    void 내_여행이면_취향을_조회할_수_있다() throws Exception {
        mvc.perform(get("/api/trips/{tripId}/preferences", tripId).header("X-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false));
    }

    @Test
    void 남의_여행이면_404다() throws Exception {
        mvc.perform(get("/api/trips/{tripId}/preferences", tripId).header("X-User-Id", otherId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 없는_여행이면_404다() throws Exception {
        mvc.perform(get("/api/trips/{tripId}/preferences", 999_999).header("X-User-Id", ownerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401이다() throws Exception {
        mvc.perform(get("/api/trips/{tripId}/preferences", tripId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }
}
