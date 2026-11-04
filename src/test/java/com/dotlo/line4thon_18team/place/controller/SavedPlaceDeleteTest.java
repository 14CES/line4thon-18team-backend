package com.dotlo.line4thon_18team.place.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장 장소 삭제 API. MySQL이 필요하다 (docker compose up -d).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "auth.dev-header-enabled=true")
@Transactional
class SavedPlaceDeleteTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired SavedPlaceRepository places;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private Long ownerId;
    private Long otherId;
    private Trip trip;
    private Trip otherTrip;

    @BeforeEach
    void setUp() {
        User owner = users.save(User.builder().googleSub("sub-owner-del").email("owner@test.com").name("주인").build());
        User other = users.save(User.builder().googleSub("sub-other-del").email("other@test.com").name("남").build());
        LocalDate start = LocalDate.of(2026, 11, 10);
        trip = trips.save(Trip.create(owner, "오사카", start, start.plusDays(2), Companion.ALONE, null));
        otherTrip = trips.save(Trip.create(other, "오사카", start, start.plusDays(2), Companion.ALONE, null));
        ownerId = owner.getId();
        otherId = other.getId();
    }

    private Long insertPlace(Trip trip, String name) {
        return places.save(SavedPlace.fromTakeout(trip, null, name, null, null, null, null, "오사카 맛집")).getId();
    }

    // 테스트 전체가 한 트랜잭션이라, JPA 삭제를 DB에 반영(flush)한 뒤에 센다
    private int count(String sql, Long id) {
        em.flush();
        return jdbc.queryForObject(sql, Integer.class, id);
    }

    @Test
    void 내_장소를_삭제하면_리스트_이름까지_지운다() throws Exception {
        Long placeId = insertPlace(trip, "이치란");

        mvc.perform(delete("/api/trips/{tripId}/places/{placeId}", trip.getId(), placeId).header("X-User-Id", ownerId))
                .andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM saved_place WHERE id = ?", placeId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM saved_place_list WHERE saved_place_id = ?", placeId)).isZero();
    }

    @Test
    void 다른_여행의_장소_id면_404이고_지우지_않는다() throws Exception {
        Long othersPlace = insertPlace(otherTrip, "남의 장소");

        mvc.perform(delete("/api/trips/{tripId}/places/{placeId}", trip.getId(), othersPlace).header("X-User-Id", ownerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAVED_PLACE_NOT_FOUND"));

        assertThat(count("SELECT COUNT(*) FROM saved_place WHERE id = ?", othersPlace)).isOne();
    }

    @Test
    void 없는_장소면_404다() throws Exception {
        mvc.perform(delete("/api/trips/{tripId}/places/{placeId}", trip.getId(), 999999L).header("X-User-Id", ownerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAVED_PLACE_NOT_FOUND"));
    }

    @Test
    void 남의_여행이면_404이고_지우지_않는다() throws Exception {
        Long placeId = insertPlace(trip, "이치란");

        mvc.perform(delete("/api/trips/{tripId}/places/{placeId}", trip.getId(), placeId).header("X-User-Id", otherId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));

        assertThat(count("SELECT COUNT(*) FROM saved_place WHERE id = ?", placeId)).isOne();
    }

    @Test
    void 로그인하지_않으면_401이다() throws Exception {
        Long placeId = insertPlace(trip, "이치란");

        mvc.perform(delete("/api/trips/{tripId}/places/{placeId}", trip.getId(), placeId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }
}
