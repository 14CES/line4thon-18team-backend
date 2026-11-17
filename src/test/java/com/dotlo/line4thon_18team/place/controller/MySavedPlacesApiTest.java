package com.dotlo.line4thon_18team.place.controller;

import com.dotlo.line4thon_18team.member.entity.TripMember;
import com.dotlo.line4thon_18team.member.repository.TripMemberRepository;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class MySavedPlacesApiTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 10);

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired TripMemberRepository members;
    @Autowired SavedPlaceRepository savedPlaces;

    @Test
    void 내가_만든_여행과_참여한_여행의_저장_장소를_최근_순으로_여행_정보와_함께_준다() throws Exception {
        User me = newUser();
        User friend = newUser();
        Trip mine = trips.save(Trip.create(me, "오사카", START, START.plusDays(2), Companion.ALONE, null));
        Trip joined = trips.save(Trip.create(friend, "교토", START, START.plusDays(1), Companion.FRIEND, 2));
        members.save(TripMember.create(joined, me));
        Trip others = trips.save(Trip.create(friend, "도쿄", START, START, Companion.ALONE, null));

        SavedPlace castle = savedPlaces.save(SavedPlace.fromSearch(mine, "오사카성", "가고 싶은 곳"));
        savedPlaces.save(SavedPlace.fromSearch(others, "남의 장소", "리스트"));
        SavedPlace temple = savedPlaces.save(SavedPlace.fromSearch(joined, "기요미즈데라", "교토"));

        mvc.perform(get("/api/users/me/places").header("X-User-Id", me.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].place.id").value(temple.getId()))
                .andExpect(jsonPath("$[0].trip.id").value(joined.getId()))
                .andExpect(jsonPath("$[0].trip.city").value("교토"))
                .andExpect(jsonPath("$[1].place.id").value(castle.getId()))
                .andExpect(jsonPath("$[1].place.name").value("오사카성"))
                .andExpect(jsonPath("$[1].place.listNames[0]").value("가고 싶은 곳"))
                .andExpect(jsonPath("$[1].trip.id").value(mine.getId()))
                .andExpect(jsonPath("$[1].trip.startDate").value("2026-12-10"));
        // 마이의 저장한 장소 수와 같은 범위
        assertThat(savedPlaces.countInMyTrips(me.getId())).isEqualTo(2);
    }

    @Test
    void 비로그인은_401() throws Exception {
        mvc.perform(get("/api/users/me/places")).andExpect(status().isUnauthorized());
    }

    private User newUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return users.save(User.builder().googleSub("sub-myplaces-" + id).email(id + "@test.com").name("테스트").build());
    }
}
