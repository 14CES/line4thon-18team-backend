package com.dotlo.line4thon_18team.itinerary.version;

import com.dotlo.line4thon_18team.itinerary.validation.model.DayDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.VisitDraft;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.itinerary.version.entity.ItinerarySource;
import com.dotlo.line4thon_18team.itinerary.version.repository.ItineraryVersionRepository;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.place.OpeningHours;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 일정 되돌리기 API. MySQL이 필요하다 (docker compose up -d).
 * 저장이 자기 트랜잭션을 따로 열어서 테스트 트랜잭션으로 되돌릴 수 없으므로, 만든 데이터는 테스트 끝에 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class ItineraryRevertApiTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 27);

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired ItineraryVersionRepository itineraryVersions;
    @Autowired ItineraryService itineraryService;

    @MockitoBean OpeningHours openingHours;
    @MockitoBean TravelTimeProvider travelTimeProvider;

    private User owner;
    private User other;
    private Trip trip;

    @BeforeEach
    void setUp() {
        owner = users.save(newUser());
        other = users.save(newUser());
        trip = trips.save(Trip.create(owner, "오사카", START, START.plusDays(1), Companion.ALONE, null));
    }

    @AfterEach
    void tearDown() {
        itineraryVersions.deleteAll(itineraryVersions.findAll().stream()
                .filter(itinerary -> itinerary.getTrip().getId().equals(trip.getId()))
                .toList());
        trips.delete(trip);
        users.delete(owner);
        users.delete(other);
    }

    @Test
    void 되돌리면_그_버전의_내용이_새_버전으로_쌓인다() throws Exception {
        saveWithVisit(ItinerarySource.AI_GENERATED, null, "v-aaa");   // v1
        saveEmpty(ItinerarySource.MANUAL_EDIT, 1);                    // v2 — 방문을 지운 상태

        revert(owner, 1).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.source").value("REVERT"))
                .andExpect(jsonPath("$.baseVersion").value(2))
                .andExpect(jsonPath("$.draft.days[0].visits.length()").value(1))
                .andExpect(jsonPath("$.draft.days[0].visits[0].itemId").value("v-aaa"))
                .andExpect(jsonPath("$.validation.summary").exists());

        // 되돌린 결과가 최신 일정이 된다. v1·v2는 지워지지 않는다
        mvc.perform(get("/api/trips/{tripId}/itineraries/latest", trip.getId())
                        .header("X-User-Id", owner.getId()))
                .andExpect(jsonPath("$.version").value(3));
        mvc.perform(get("/api/trips/{tripId}/itineraries", trip.getId())
                        .header("X-User-Id", owner.getId()))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void 되돌리기를_다시_되돌릴_수_있다() throws Exception {
        saveWithVisit(ItinerarySource.AI_GENERATED, null, "v-aaa");   // v1
        saveEmpty(ItinerarySource.MANUAL_EDIT, 1);                    // v2
        revert(owner, 1).andExpect(status().isOk());                  // v3 = v1 내용

        revert(owner, 2).andExpect(status().isOk())                   // v4 = v2 내용
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.source").value("REVERT"))
                .andExpect(jsonPath("$.baseVersion").value(3))
                .andExpect(jsonPath("$.draft.days[0].visits.length()").value(0));
    }

    @Test
    void 최신_버전으로_되돌리면_400이다() throws Exception {
        saveWithVisit(ItinerarySource.AI_GENERATED, null, "v-aaa");   // v1

        revert(owner, 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITINERARY_ALREADY_LATEST"));
    }

    @Test
    void 없는_버전으로_되돌리면_404다() throws Exception {
        saveWithVisit(ItinerarySource.AI_GENERATED, null, "v-aaa");   // v1

        revert(owner, 9).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITINERARY_VERSION_NOT_FOUND"));
    }

    @Test
    void 남의_여행이면_404다() throws Exception {
        saveWithVisit(ItinerarySource.AI_GENERATED, null, "v-aaa");
        saveEmpty(ItinerarySource.MANUAL_EDIT, 1);

        revert(other, 1).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    private void saveWithVisit(ItinerarySource source, Integer baseVersion, String itemId) {
        itineraryService.save(trip.getId(), owner.getId(), source, baseVersion,
                ItineraryDraft.of(DayDraft.of(START, VisitDraft.of(itemId, "ChIJ-a"))),
                ValidationResult.of(List.of(), List.of()));
    }

    private void saveEmpty(ItinerarySource source, Integer baseVersion) {
        itineraryService.save(trip.getId(), owner.getId(), source, baseVersion,
                ItineraryDraft.of(DayDraft.of(START)),
                ValidationResult.of(List.of(), List.of()));
    }

    private ResultActions revert(User user, int version) throws Exception {
        return mvc.perform(post("/api/trips/{tripId}/itineraries/{version}/revert", trip.getId(), version)
                .header("X-User-Id", user.getId()));
    }

    private static User newUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return User.builder().googleSub("sub-rev-" + id).email(id + "@test.com").name("되돌리기테스트").build();
    }
}
