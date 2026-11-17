package com.dotlo.line4thon_18team.place.controller;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 MySQL 집계. 다른 테스트 데이터와 섞이지 않게 테스트마다 이름이 다른 도시를 쓴다. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class PopularPlacesApiTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 10);

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired SavedPlaceRepository savedPlaces;

    private final String city = "테스트시-" + UUID.randomUUID().toString().substring(0, 8);
    private User me;
    private Trip myTrip;

    @BeforeEach
    void setUp() {
        me = newUser();
        myTrip = newTrip(city);
    }

    @Test
    void 많은_여행이_담은_순으로_주고_내_여행에_있는_곳_확인_안_된_곳_다른_도시는_뺀다() throws Exception {
        save(newTrip(city), "castle", "옛 이름", PlaceMatchStatus.MATCHED);
        save(newTrip(city), "castle", "오사카성", PlaceMatchStatus.MATCHED);
        Trip both = newTrip(city);
        save(both, "castle", "오사카성", PlaceMatchStatus.MATCHED);
        save(both, "ramen", "이치란", PlaceMatchStatus.MATCHED);
        IntStream.range(0, 2).forEach(i -> save(newTrip(city), "mine", "내가 담은 곳", PlaceMatchStatus.MATCHED));
        save(myTrip, "mine", "내가 담은 곳", PlaceMatchStatus.MATCHED);
        IntStream.range(0, 4).forEach(i -> save(newTrip(city), "review", "확인 필요", PlaceMatchStatus.NEEDS_REVIEW));
        IntStream.range(0, 4).forEach(i -> save(newTrip("다른-" + city), "other", "다른 도시", PlaceMatchStatus.MATCHED));

        popular(me, myTrip)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].placeId").value(placeId("castle")))
                .andExpect(jsonPath("$[0].name").value("오사카성"))   // 가장 최근에 담긴 행의 이름
                .andExpect(jsonPath("$[0].savedCount").value(3))
                .andExpect(jsonPath("$[0].category").value("CULTURE"))
                .andExpect(jsonPath("$[0].latitude").value(34.68))
                .andExpect(jsonPath("$[1].placeId").value(placeId("ramen")))
                .andExpect(jsonPath("$[1].savedCount").value(1));
    }

    @Test
    void 최대_10개() throws Exception {
        IntStream.range(0, 11).forEach(i -> save(newTrip(city), "p" + i, "장소 " + i, PlaceMatchStatus.MATCHED));

        popular(me, myTrip).andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    void 담은_곳이_없으면_빈_목록_구성원이_아니면_404() throws Exception {
        popular(me, myTrip).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        popular(newUser(), myTrip)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    private ResultActions popular(User user, Trip trip) throws Exception {
        return mvc.perform(get("/api/places/popular").param("tripId", trip.getId().toString())
                .header("X-User-Id", user.getId()));
    }

    private void save(Trip trip, String key, String name, PlaceMatchStatus status) {
        SavedPlace place = SavedPlace.fromSearch(trip, name, "리스트");
        place.applyMatch(name, placeId(key), "주소", 34.68, 135.52, PlaceCategory.CULTURE, status);
        savedPlaces.save(place);
    }

    private String placeId(String key) {
        return city + "-" + key;
    }

    private Trip newTrip(String tripCity) {
        return trips.save(Trip.create(me, tripCity, START, START.plusDays(2), Companion.ALONE, null));
    }

    private User newUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return users.save(User.builder().googleSub("sub-popular-" + id).email(id + "@test.com").name("테스트").build());
    }
}
