package com.dotlo.line4thon_18team.place;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.itinerary.ItineraryTripCleanupService;
import com.dotlo.line4thon_18team.place.PlaceMatcher.Candidate;
import com.dotlo.line4thon_18team.place.PlaceMatcher.MatchResult;
import com.dotlo.line4thon_18team.place.PlaceMatcher.MatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.importer.ImportedList;
import com.dotlo.line4thon_18team.place.importer.ImportedPlace;
import com.dotlo.line4thon_18team.place.link.GoogleListClient;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.place.service.PlaceEnrichService;
import com.dotlo.line4thon_18team.place.service.SavedPlaceService;
import com.dotlo.line4thon_18team.reservation.dto.FlightInfo;
import com.dotlo.line4thon_18team.reservation.dto.StaysRequest.Stay;
import com.dotlo.line4thon_18team.reservation.service.ReservationService;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.trip.service.TripDeleteService;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 여행을 지우는 도중에 BE2 쓰기(저장 장소, 예약)가 들어오면 500 이 아니라 404 가 되는지.
 * 삭제가 여행 행을 잠근 채 멈춰 있는 사이 쓰기를 보낸다. 요청마다 트랜잭션이 따로 열려야 해서 테스트 트랜잭션 없이 돌린다.
 */
@SpringBootTest
class Be2TripDeleteRaceTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 10);
    private static final String CSV = """
            제목,메모,URL,태그,댓글

            도톤보리,,https://www.google.com/maps/place/x/data=!4m2!3m1!1s0x6000e7130b0e1d3f:0x1d3f0b0e1300e760,,
            """;

    @Autowired SavedPlaceService savedPlaceService;
    @Autowired PlaceEnrichService placeEnrichService;
    @Autowired ReservationService reservationService;
    @Autowired TripDeleteService tripDeleteService;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired SavedPlaceRepository savedPlaces;

    @MockitoBean PlaceMatcher placeMatcher;
    @MockitoBean GoogleListClient googleListClient;
    @MockitoSpyBean ItineraryTripCleanupService itineraryCleanup;   // 삭제를 잠금 잡은 채 멈추게 하는 자리

    private User owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        owner = users.save(User.builder().googleSub("sub-be2race-" + id).email(id + "@test.com").name("테스트").build());
        trip = trips.save(Trip.create(owner, "오사카", START, START.plusDays(2), Companion.ALONE, null));

        Candidate place = new Candidate("test-place", "테스트 장소", "오사카", 34.67, 135.50);
        when(placeMatcher.details(any(), anyString()))
                .thenReturn(new MatchResult(null, MatchStatus.MATCHED, place, List.of(), PlaceCategory.FOOD, false));
        when(googleListClient.fetch(anyString())).thenReturn(new ImportedList("공유 리스트",
                List.of(new ImportedPlace("도톤보리", null, "https://maps.google.com/?cid=1", "0x1:0x2", 34.67, 135.50)), 0));
    }

    @AfterEach
    void tearDown() {
        if (trips.existsById(trip.getId())) {
            tripDeleteService.deleteTrip(owner.getId(), trip.getId());
        }
        users.delete(owner);
    }

    @Test
    void 항공편() {
        FlightInfo.Point icn = new FlightInfo.Point("ICN", LocalDateTime.of(2026, 12, 10, 9, 0), 37.46, 126.44);
        FlightInfo.Point kix = new FlightInfo.Point("KIX", LocalDateTime.of(2026, 12, 10, 11, 0), 34.43, 135.23);
        writeWhileDeleting(() -> reservationService.putFlights(owner.getId(), trip.getId(),
                new FlightInfo("KE723", "대한항공", icn, kix), null));
    }

    @Test
    void 숙소() {
        writeWhileDeleting(() -> reservationService.putStays(owner.getId(), trip.getId(),
                List.of(new Stay("test-hotel", START, START.plusDays(1)))));
    }

    @Test
    void 렌터카() {
        writeWhileDeleting(() -> reservationService.putRental(owner.getId(), trip.getId(),
                List.of(START), "test-rental"));
    }

    @Test
    void 장소_직접_추가() {
        writeWhileDeleting(() -> placeEnrichService.add(owner.getId(), trip.getId(), "test-place"));
    }

    @Test
    void Takeout_업로드() {
        writeWhileDeleting(() -> savedPlaceService.uploadTakeout(owner.getId(), trip.getId(),
                new MockMultipartFile("file", "오사카.csv", "text/csv", CSV.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void 공유_리스트_가져오기() {
        writeWhileDeleting(() -> savedPlaceService.importSharedList(owner.getId(), trip.getId(),
                "https://maps.app.goo.gl/test"));
    }

    @Test
    void 저장_장소_삭제() {
        long placeId = savedPlaces.save(SavedPlace.fromSearch(trip, "지울 장소", "직접 추가")).getId();
        writeWhileDeleting(() -> savedPlaceService.deletePlace(owner.getId(), trip.getId(), placeId));
    }

    /** 여행 삭제가 여행 행을 잠근 채 멈춰 있을 때 쓰기를 보낸다. 쓰기는 삭제가 끝난 뒤 404 여야 하고, 삭제는 성공해야 한다. */
    private void writeWhileDeleting(ThrowingCallable write) {
        CountDownLatch locked = new CountDownLatch(1);
        doAnswer(invocation -> {
            locked.countDown();
            Thread.sleep(500);
            return invocation.callRealMethod();
        }).when(itineraryCleanup).deleteByTrip(anyLong());

        CompletableFuture<Void> deleting = CompletableFuture.runAsync(
                () -> tripDeleteService.deleteTrip(owner.getId(), trip.getId()));
        await(locked);
        Throwable thrown = catchThrowable(write);

        deleting.join();
        assertThat(trips.existsById(trip.getId())).isFalse();
        assertThat(thrown).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TRIP_NOT_FOUND);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
