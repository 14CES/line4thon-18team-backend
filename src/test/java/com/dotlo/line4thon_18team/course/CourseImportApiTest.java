package com.dotlo.line4thon_18team.course;

import com.dotlo.line4thon_18team.course.entity.Course;
import com.dotlo.line4thon_18team.course.entity.CourseDay;
import com.dotlo.line4thon_18team.course.repository.CourseRepository;
import com.dotlo.line4thon_18team.itinerary.ItineraryValidationService;
import com.dotlo.line4thon_18team.itinerary.validation.port.TravelTimeProvider;
import com.dotlo.line4thon_18team.place.OpeningHours;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.PlaceSource;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.trip.service.TripDeleteService;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인기 코스 가져오기. 서비스가 트랜잭션을 직접 나눠 열어서 테스트 트랜잭션 없이 실제로 커밋하고 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class CourseImportApiTest {

    private static final String URL = "/api/courses/{courseId}/import";
    private static final LocalDate START = LocalDate.of(2026, 12, 10);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CourseRepository courses;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired SavedPlaceRepository savedPlaces;
    @Autowired TripDeleteService tripDeleteService;

    @MockitoBean OpeningHours openingHours;
    @MockitoBean TravelTimeProvider travelTimeProvider;
    @MockitoSpyBean ItineraryValidationService validationService;

    private User user;
    private Course course;
    private final String tag = UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void setUp() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        user = users.save(User.builder().googleSub("sub-course-" + id).email(id + "@test.com").name("테스트").build());

        course = Course.create("오사카 먹방 코스", "일본", "오사카", 2);
        CourseDay day1 = course.addDay(1);
        day1.addStop(placeId("castle"), "오사카성", 34.6873, 135.5262, PlaceCategory.CULTURE, null);
        day1.addStop(placeId("dotonbori"), "도톤보리", 34.6687, 135.5013, PlaceCategory.FOOD, null);
        course.addDay(2).addStop(placeId("castle"), "오사카성", 34.6873, 135.5262, PlaceCategory.CULTURE, null);
        course = courses.saveAndFlush(course);
    }

    @AfterEach
    void tearDown() {
        myTrips().forEach(trip -> tripDeleteService.deleteTrip(user.getId(), trip.getId()));
        courses.deleteById(course.getId());
        users.delete(user);
    }

    @Test
    void 코스_기간만큼_새_여행을_만들고_N일차를_그대로_넣는다() throws Exception {
        JsonNode body = mapper.readTree(importCourse(course.getId(), "{\"startDate\":\"2026-12-10\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itinerary.version").value(1))
                .andExpect(jsonPath("$.itinerary.source").value("COURSE_ADD"))
                .andReturn().getResponse().getContentAsString());
        long tripId = body.get("tripId").asLong();

        Trip trip = trips.findById(tripId).orElseThrow();
        assertThat(trip.getUser().getId()).isEqualTo(user.getId());
        assertThat(trip.getCity()).isEqualTo("오사카");
        assertThat(trip.getStartDate()).isEqualTo(START);
        assertThat(trip.getEndDate()).isEqualTo(START.plusDays(1));
        assertThat(trip.getCompanion()).isEqualTo(Companion.ALONE);

        JsonNode days = body.get("itinerary").get("draft").get("days");
        assertThat(days).hasSize(2);
        assertThat(days.get(0).get("date").asString()).isEqualTo("2026-12-10");
        assertThat(days.get(0).get("visits").findValuesAsString("placeId"))
                .containsExactly(placeId("castle"), placeId("dotonbori"));
        assertThat(days.get(1).get("date").asString()).isEqualTo("2026-12-11");
        assertThat(days.get(1).get("visits").findValuesAsString("placeId")).containsExactly(placeId("castle"));
        // 같은 장소를 두 번 방문해도 일정 항목은 따로다
        assertThat(days.findValuesAsString("itemId")).doesNotHaveDuplicates().hasSize(3);

        // 저장 장소는 장소마다 1행
        List<SavedPlace> places = savedPlaces.findAllByTripIdOrderByIdAsc(tripId);
        assertThat(places).extracting(SavedPlace::getPlaceId).containsExactly(placeId("castle"), placeId("dotonbori"));
        SavedPlace castle = places.get(0);
        assertThat(castle.getSource()).isEqualTo(PlaceSource.POPULAR);
        assertThat(castle.getMatchStatus()).isEqualTo(PlaceMatchStatus.MATCHED);
        assertThat(castle.getCategory()).isEqualTo(PlaceCategory.CULTURE);
        assertThat(castle.getLatitude()).isEqualTo(34.6873);
        assertThat(castle.getListNames()).containsExactly("오사카 먹방 코스");
    }

    @Test
    void 동행을_보내면_그대로_쓴다() throws Exception {
        importCourse(course.getId(), "{\"startDate\":\"2026-12-10\",\"companion\":\"FRIEND\",\"companionCount\":3}")
                .andExpect(status().isCreated());

        assertThat(myTrips()).singleElement().satisfies(trip -> {
            assertThat(trip.getCompanion()).isEqualTo(Companion.FRIEND);
            assertThat(trip.getCompanionCount()).isEqualTo(3);
        });
    }

    @Test
    void 비어_있는_일차가_있어도_여행은_코스_기간만큼이고_있는_날만_채운다() throws Exception {
        Course gap = Course.create("빈 날 있는 코스", "일본", "오사카", 3);
        gap.addDay(3).addStop(placeId("castle"), "오사카성", 34.6873, 135.5262, PlaceCategory.CULTURE, null);
        gap.addDay(1).addStop(placeId("dotonbori"), "도톤보리", 34.6687, 135.5013, PlaceCategory.FOOD, null);
        gap = courses.saveAndFlush(gap);
        try {
            JsonNode days = mapper.readTree(importCourse(gap.getId(), "{\"startDate\":\"2026-12-10\"}")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString()).get("itinerary").get("draft").get("days");

            assertThat(days.findValuesAsString("date")).containsExactly("2026-12-10", "2026-12-12");
            assertThat(myTrips()).singleElement()
                    .satisfies(trip -> assertThat(trip.getEndDate()).isEqualTo(START.plusDays(2)));
        } finally {
            myTrips().forEach(trip -> tripDeleteService.deleteTrip(user.getId(), trip.getId()));
            courses.deleteById(gap.getId());
        }
    }

    @Test
    void 지원하지_않는_도시의_코스는_400이고_여행을_만들지_않는다() throws Exception {
        Course paris = Course.create("파리 코스", "프랑스", "파리", 1);
        paris.addDay(1).addStop(placeId("louvre"), "루브르", 48.86, 2.33, PlaceCategory.CULTURE, null);
        paris = courses.saveAndFlush(paris);
        try {
            importCourse(paris.getId(), "{\"startDate\":\"2026-12-10\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_CITY"));
            assertThat(myTrips()).isEmpty();
        } finally {
            courses.deleteById(paris.getId());
        }
    }

    @Test
    void 일정_저장_전에_실패하면_만든_여행을_지운다() throws Exception {
        doThrow(new IllegalStateException("검증 실패")).when(validationService)
                .validate(anyLong(), anyLong(), any(), any());

        try {
            importCourse(course.getId(), "{\"startDate\":\"2026-12-10\"}");
        } catch (Exception ignored) {
            // 예상 못 한 예외라 500 이든 그대로 던지든 상관없다. 여행이 남지 않는지만 본다
        }

        assertThat(myTrips()).isEmpty();
    }

    @Test
    void 없는_코스_시작일_누락_비로그인은_여행을_만들지_않는다() throws Exception {
        importCourse(-1L, "{\"startDate\":\"2026-12-10\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
        importCourse(course.getId(), "{}")
                .andExpect(status().isBadRequest());
        mvc.perform(post(URL, course.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-12-10\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(myTrips()).isEmpty();
    }

    private ResultActions importCourse(long courseId, String json) throws Exception {
        return mvc.perform(post(URL, courseId).header("X-User-Id", user.getId())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private List<Trip> myTrips() {
        return trips.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    private String placeId(String name) {
        return "test-" + name + "-" + tag;
    }
}
