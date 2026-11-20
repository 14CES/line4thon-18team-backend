package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.ItineraryValidator;
import com.dotlo.line4thon_18team.itinerary.validation.ValidationPolicy;
import com.dotlo.line4thon_18team.itinerary.validation.model.DayDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ValidationOrigin;
import com.dotlo.line4thon_18team.itinerary.validation.model.VisitDraft;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.itinerary.validation.support.FakeTravelTimeProvider;
import com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.CAFE_CLOSED_EVERY_DAY;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.CAFE_CLOSED_MONDAY;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.DAY1;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.DAY2;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.DAY3;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.DAY4;
import static com.dotlo.line4thon_18team.itinerary.validation.support.OsakaFixtures.DOTONBORI;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 서버가 방문을 옮겨서 검증 문제를 고치는 부분. 1일차는 월요일(2026-12-14)이다.
 */
class ItineraryRepairerTest {

    private final Function<ItineraryDraft, ValidationResult> check = draft ->
            new ItineraryValidator(OsakaFixtures.places(), FakeTravelTimeProvider.fixedMinutes(15),
                    ValidationPolicy.defaults(), false)
                    .validate(draft, OsakaFixtures.trip(), PreferenceProfile.defaults(),
                            ValidationOrigin.AI_GENERATED);

    @Test
    void 그날_휴무인_장소를_다른_날로_옮긴다() {
        ItineraryDraft draft = days(
                List.of(VisitDraft.of("v-1", DOTONBORI.placeId()),
                        VisitDraft.of("v-2", CAFE_CLOSED_MONDAY.placeId())),
                List.of(), List.of(), List.of());

        ItineraryRepairer.Repair repaired = ItineraryRepairer.repair(draft, check);

        assertThat(check.apply(repaired.draft()).summary().valid()).isTrue();
        assertThat(placeIdsOn(repaired.draft(), DAY1)).containsExactly(DOTONBORI.placeId());
        assertThat(placeIdsOn(repaired.draft(), DAY2)).containsExactly(CAFE_CLOSED_MONDAY.placeId());
        assertThat(repaired.droppedPlaceIds()).isEmpty();
    }

    @Test
    void 장소를_빼지_않고_옮기기만_한다() {
        ItineraryDraft draft = days(
                List.of(VisitDraft.of("v-1", DOTONBORI.placeId()),
                        VisitDraft.of("v-2", CAFE_CLOSED_MONDAY.placeId())),
                List.of(), List.of(), List.of());

        ItineraryRepairer.Repair repaired = ItineraryRepairer.repair(draft, check);

        assertThat(repaired.draft().days()).hasSize(4);
        assertThat(repaired.draft().days()).flatExtracting(DayDraft::visits).extracting(VisitDraft::placeId)
                .containsExactlyInAnyOrder(DOTONBORI.placeId(), CAFE_CLOSED_MONDAY.placeId());
    }

    @Test
    void 옮겨도_풀리지_않으면_그_장소를_뺀다() {
        ItineraryDraft draft = days(
                List.of(VisitDraft.of("v-1", DOTONBORI.placeId()),
                        VisitDraft.of("v-2", CAFE_CLOSED_EVERY_DAY.placeId())),
                List.of(), List.of(), List.of());

        ItineraryRepairer.Repair repaired = ItineraryRepairer.repair(draft, check);

        assertThat(repaired.droppedPlaceIds()).containsExactly(CAFE_CLOSED_EVERY_DAY.placeId());
        assertThat(check.apply(repaired.draft()).summary().valid()).isTrue();
        assertThat(repaired.draft().days()).flatExtracting(DayDraft::visits).extracting(VisitDraft::placeId)
                .containsExactly(DOTONBORI.placeId());
    }

    @Test
    void 옮겨서_풀리는_문제는_장소를_빼지_않는다() {
        ItineraryDraft draft = days(
                List.of(VisitDraft.of("v-1", DOTONBORI.placeId()),
                        VisitDraft.of("v-2", CAFE_CLOSED_MONDAY.placeId())),
                List.of(), List.of(), List.of());

        assertThat(ItineraryRepairer.repair(draft, check).droppedPlaceIds()).isEmpty();
    }

    @Test
    void 문제가_없으면_그대로_돌려준다() {
        ItineraryDraft draft = days(List.of(VisitDraft.of("v-1", DOTONBORI.placeId())),
                List.of(), List.of(), List.of());

        ItineraryRepairer.Repair repaired = ItineraryRepairer.repair(draft, check);

        assertThat(repaired.draft()).isSameAs(draft);
        assertThat(repaired.droppedPlaceIds()).isEmpty();
    }

    private static ItineraryDraft days(List<VisitDraft> day1, List<VisitDraft> day2, List<VisitDraft> day3,
                                       List<VisitDraft> day4) {
        return new ItineraryDraft(List.of(new DayDraft(DAY1, day1), new DayDraft(DAY2, day2),
                new DayDraft(DAY3, day3), new DayDraft(DAY4, day4)));
    }

    private static List<String> placeIdsOn(ItineraryDraft draft, LocalDate date) {
        return draft.days().stream()
                .filter(day -> day.date().equals(date))
                .flatMap(day -> day.visits().stream())
                .map(VisitDraft::placeId)
                .toList();
    }
}
