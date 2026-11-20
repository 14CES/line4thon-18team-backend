package com.dotlo.line4thon_18team.itinerary;

import com.dotlo.line4thon_18team.itinerary.validation.model.OpeningHours;
import com.dotlo.line4thon_18team.itinerary.validation.model.WeeklyPeriod;
import com.dotlo.line4thon_18team.place.OpeningHours.Hours;
import com.dotlo.line4thon_18team.place.OpeningHours.Range;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** BE2 영업시간(요일별 분 단위 구간)을 검증기 영업시간으로 바꾸는 부분. */
class SavedPlaceInfoProviderTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    @Test
    void 하루_영업시간을_옮긴다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(
                days(Map.of(DayOfWeek.MONDAY, List.of(new Range(10 * 60, 22 * 60)))));

        assertThat(hours.periods()).containsExactly(
                new WeeklyPeriod(DayOfWeek.MONDAY, LocalTime.of(10, 0), DayOfWeek.MONDAY, LocalTime.of(22, 0)));
        assertThat(hours.describe(MONDAY)).isEqualTo("10:00-22:00");
    }

    @Test
    void 브레이크_타임은_구간_두_개가_된다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(days(Map.of(
                DayOfWeek.MONDAY, List.of(new Range(11 * 60, 14 * 60), new Range(17 * 60, 22 * 60)))));

        assertThat(hours.describe(MONDAY)).isEqualTo("11:00-14:00, 17:00-22:00");
    }

    @Test
    void 자정을_넘기면_다음_날까지로_본다() {
        // 월 17:00 ~ 화 02:00 → [1020, 1560]
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(
                days(Map.of(DayOfWeek.MONDAY, List.of(new Range(17 * 60, 26 * 60)))));

        assertThat(hours.periods()).containsExactly(
                new WeeklyPeriod(DayOfWeek.MONDAY, LocalTime.of(17, 0), DayOfWeek.TUESDAY, LocalTime.of(2, 0)));
        assertThat(hours.covers(MONDAY.atTime(23, 0), MONDAY.plusDays(1).atTime(1, 0))).isTrue();
    }

    @Test
    void 휴무일은_그날_영업_구간이_없다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(
                days(Map.of(DayOfWeek.TUESDAY, List.of(new Range(10 * 60, 18 * 60)))));

        assertThat(hours.isClosedOn(MONDAY)).isTrue();
        assertThat(hours.isClosedOn(MONDAY.plusDays(1))).isFalse();
    }

    @Test
    void _24시간_영업은_언제나_열려_있다() {
        OpeningHours hours = SavedPlaceInfoProvider.toOpeningHours(new Hours(true, Map.of()));

        assertThat(hours.isAlwaysOpen()).isTrue();
        LocalDateTime dawn = MONDAY.atTime(3, 0);
        assertThat(hours.earliestStart(dawn, 60)).contains(dawn);
    }

    @Test
    void 영업시간은_placeId마다_한_번만_조회한다() {
        AtomicInteger calls = new AtomicInteger();
        SavedPlaceInfoProvider provider = provider(calls,
                days(Map.of(DayOfWeek.MONDAY, List.of(new Range(10 * 60, 22 * 60)))));

        provider.findPlaces(List.of("p1"));
        provider.findPlaces(List.of("p1"));
        provider.findPlaces(List.of("p1"));

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void 영업시간이_없는_장소도_다시_조회하지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        SavedPlaceInfoProvider provider = provider(calls, null);

        provider.findPlaces(List.of("p1"));
        provider.findPlaces(List.of("p1"));

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void 미리_조회하면_여러_장소를_동시에_한_번씩_조회한다() {
        // 세 조회가 서로를 기다린다. 순서대로 조회하면 첫 조회가 시간 안에 짝을 못 만나 false가 된다
        CountDownLatch together = new CountDownLatch(3);
        List<Boolean> metTogether = new CopyOnWriteArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        SavedPlaceInfoProvider provider = new SavedPlaceInfoProvider(List.of(), placeId -> {
            calls.incrementAndGet();
            together.countDown();
            try {
                metTogether.add(together.await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return days(Map.of(DayOfWeek.MONDAY, List.of(new Range(10 * 60, 22 * 60))));
        });

        provider.prefetchHours(List.of("p1", "p2", "p3", "p1"));

        assertThat(metTogether).containsExactly(true, true, true);
        assertThat(provider.hoursOf("p2").describe(MONDAY)).isEqualTo("10:00-22:00");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void 미리_조회에_실패한_장소는_미확인으로_두고_나머지는_채운다() {
        AtomicInteger calls = new AtomicInteger();
        SavedPlaceInfoProvider provider = new SavedPlaceInfoProvider(List.of(), placeId -> {
            calls.incrementAndGet();
            if (placeId.equals("p2")) {
                throw new IllegalStateException("할당량");
            }
            return days(Map.of(DayOfWeek.MONDAY, List.of(new Range(10 * 60, 22 * 60))));
        });

        provider.prefetchHours(List.of("p1", "p2", "p3"));

        assertThat(provider.hoursOf("p1")).isNotNull();
        assertThat(provider.hoursOf("p2")).isNull();
        assertThat(provider.hoursOf("p3")).isNotNull();
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void 이미_조회한_장소는_미리_조회에서_다시_부르지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        SavedPlaceInfoProvider provider = new SavedPlaceInfoProvider(List.of(), placeId -> {
            calls.incrementAndGet();
            return null;
        });

        provider.hoursOf("p1");
        provider.prefetchHours(List.of("p1", "p2", "p3"));

        assertThat(calls.get()).isEqualTo(3);
    }

    private static SavedPlaceInfoProvider provider(AtomicInteger calls, Hours hours) {
        SavedPlace place = Mockito.mock(SavedPlace.class);
        Mockito.when(place.getPlaceId()).thenReturn("p1");
        Mockito.when(place.getName()).thenReturn("가게");
        return new SavedPlaceInfoProvider(List.of(place), placeId -> {
            calls.incrementAndGet();
            return hours;
        });
    }

    // BE2 Hours 는 모든 요일 키를 채워서 준다 (휴무인 요일은 빈 리스트)
    private static Hours days(Map<DayOfWeek, List<Range>> open) {
        Map<DayOfWeek, List<Range>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, new ArrayList<>(open.getOrDefault(day, List.of())));
        }
        return new Hours(false, days);
    }
}
