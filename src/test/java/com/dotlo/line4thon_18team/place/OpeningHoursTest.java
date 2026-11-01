package com.dotlo.line4thon_18team.place;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

import com.dotlo.line4thon_18team.place.OpeningHours.Hours;
import com.dotlo.line4thon_18team.place.OpeningHours.Period;
import com.dotlo.line4thon_18team.place.OpeningHours.Point;
import com.dotlo.line4thon_18team.place.OpeningHours.Range;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// 케이스는 오사카 실제 장소의 API 응답 형태 그대로 (2026-11-01 확인)
class OpeningHoursTest {

    // 요일(0=일)마다 같은 시간, skip 요일은 휴무
    private static List<Period> daily(int oh, int om, int ch, int cm, boolean nextDay, int... skip) {
        List<Period> l = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            final int day = d;
            if (java.util.Arrays.stream(skip).anyMatch(x -> x == day)) continue;
            l.add(new Period(new Point(d, oh, om), new Point(nextDay ? (d + 1) % 7 : d, ch, cm)));
        }
        return l;
    }

    @Test
    void 정기_휴무일() {  // 오사카 역사박물관: 09:30~17:00, 화요일 휴무
        Hours h = OpeningHours.toHours(daily(9, 30, 17, 0, false, 2));
        assertFalse(h.open24h());
        assertEquals(List.of(new Range(570, 1020)), h.days().get(MONDAY));
        assertTrue(h.days().get(TUESDAY).isEmpty());
        assertTrue(h.isOpen(MONDAY, LocalTime.of(10, 0), 120));
        assertFalse(h.isOpen(TUESDAY, LocalTime.of(10, 0), 120));
        assertFalse(h.isOpen(MONDAY, LocalTime.of(16, 0), 90));  // 17:30까지 있으면 문 닫음
    }

    @Test
    void 브레이크_타임() {  // 치토세: 11:30~14:30, 17:00~20:00
        List<Period> p = new ArrayList<>(daily(11, 30, 14, 30, false));
        p.addAll(daily(17, 0, 20, 0, false));
        Hours h = OpeningHours.toHours(p);
        assertEquals(2, h.days().get(FRIDAY).size());
        assertTrue(h.isOpen(FRIDAY, LocalTime.of(12, 0), 60));
        assertFalse(h.isOpen(FRIDAY, LocalTime.of(15, 0), 60));   // 브레이크 타임
        assertFalse(h.isOpen(FRIDAY, LocalTime.of(14, 0), 60));   // 걸쳐 있음
    }

    @Test
    void 자정_넘김() {  // 모미지: 17:00~다음날 02:00, 토요일은 일요일(0) 02:00에 닫음
        Hours h = OpeningHours.toHours(daily(17, 0, 2, 0, true));
        assertEquals(List.of(new Range(1020, 1560)), h.days().get(SATURDAY));
        assertTrue(h.isOpen(SATURDAY, LocalTime.of(23, 0), 120));  // 01:00까지
        assertTrue(h.isOpen(SUNDAY, LocalTime.of(1, 0), 30));      // 토요일 밤 영업이 이어짐
        assertFalse(h.isOpen(SUNDAY, LocalTime.of(3, 0), 30));
    }

    @Test
    void 영업시간_24시간() {  // close 없이 open(일 0:00)만
        Hours h = OpeningHours.toHours(List.of(new Period(new Point(0, 0, 0), null)));
        assertTrue(h.open24h());
        assertTrue(h.isOpen(WEDNESDAY, LocalTime.of(23, 0), 180));
    }

    // 실제 API 호출. 키가 환경변수에 있을 때만 (CI에서는 건너뜀)
    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_MAPS_API_KEY", matches = ".+")
    void 실제_API() {
        OpeningHours service = new OpeningHours(System.getenv("GOOGLE_MAPS_API_KEY"));
        Hours h = service.get("ChIJt9Y375HmAGARejKVNQwlkbI");  // 이치란 우메다시바타점, 매일 10:00~22:45
        assertEquals(List.of(new Range(600, 1365)), h.days().get(MONDAY));
        assertTrue(h.isOpen(MONDAY, LocalTime.of(12, 0), 60));
        assertFalse(h.isOpen(MONDAY, LocalTime.of(22, 30), 60));
    }
}
