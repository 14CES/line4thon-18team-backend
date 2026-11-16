package com.dotlo.line4thon_18team.place.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.dotlo.line4thon_18team.place.OpeningHours.Hours;
import com.dotlo.line4thon_18team.place.OpeningHours.Range;
import com.dotlo.line4thon_18team.place.dto.PlaceHoursResponse.Period;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaceHoursResponseTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    @Test
    void 요일별_시각과_자정_넘는_영업() {
        // 월: 11:00~14:00, 17:00~02:00 (브레이크 타임, 자정 넘김) / 화: 휴무 / 나머지 17:00~24:00
        Map<DayOfWeek, List<Range>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) days.put(d, new ArrayList<>(List.of(new Range(1020, 1440))));
        days.put(DayOfWeek.MONDAY, List.of(new Range(660, 840), new Range(1020, 1560)));
        days.put(DayOfWeek.TUESDAY, List.of());
        Hours hours = new Hours(false, days);

        PlaceHoursResponse res = PlaceHoursResponse.of(hours, at(2026, 11, 16, 12, 0));   // 월요일 12시

        assertThat(res.days()).extracting(PlaceHoursResponse.Day::day).startsWith(DayOfWeek.MONDAY).hasSize(7);
        assertThat(res.days().get(0).hours()).containsExactly(new Period("11:00", "14:00"), new Period("17:00", "02:00"));
        assertThat(res.days().get(1).hours()).isEmpty();
        assertThat(res.days().get(2).hours()).containsExactly(new Period("17:00", "24:00"));
        assertThat(res.openNow()).isTrue();

        assertThat(PlaceHoursResponse.of(hours, at(2026, 11, 16, 15, 0)).openNow()).isFalse();   // 월 브레이크 타임
        assertThat(PlaceHoursResponse.of(hours, at(2026, 11, 17, 1, 30)).openNow()).isTrue();    // 화 새벽, 월요일 밤 영업
        assertThat(PlaceHoursResponse.of(hours, at(2026, 11, 17, 18, 0)).openNow()).isFalse();   // 화 휴무
    }

    @Test
    void 연중무휴_24시간() {
        Map<DayOfWeek, List<Range>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) days.put(d, List.of(new Range(0, 1440)));

        PlaceHoursResponse res = PlaceHoursResponse.of(new Hours(true, days), at(2026, 11, 16, 3, 0));

        assertThat(res.open24h()).isTrue();
        assertThat(res.openNow()).isTrue();
        assertThat(res.days().get(0).hours()).containsExactly(new Period("00:00", "24:00"));
    }

    private static ZonedDateTime at(int y, int m, int d, int h, int min) {
        return ZonedDateTime.of(y, m, d, h, min, 0, 0, TOKYO);
    }
}
