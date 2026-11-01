package com.dotlo.line4thon_18team.place;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** BE2 영업시간. BE3(일정 검증)는 이 서비스를 주입받아 직접 호출합니다. */
@Service
public class OpeningHours {

    /*
    - open24h: 24시간 영업이면 true. 이때 days는 매일 [0, 1440].
    - days: 요일별 영업 구간. 휴무인 요일은 빈 리스트, 브레이크 타임이 있으면 구간 2개.
    */
    public record Hours(boolean open24h, Map<DayOfWeek, List<Range>> days) {

        // 방문 시작부터 minutes분 동안 계속 영업 중인지. 시각은 현지 시간입니다.
        public boolean isOpen(DayOfWeek day, LocalTime start, int minutes) {
            if (open24h) return true;
            int s = start.getHour() * 60 + start.getMinute();
            int e = s + minutes;
            for (Range r : days.get(day)) {
                if (r.start() <= s && e <= r.end()) return true;
            }
            // 전날 밤부터 이어지는 구간 (예: 전날 17:00~02:00 → [1020, 1560])
            for (Range r : days.get(day.minus(1))) {
                if (r.start() <= s + 1440 && e + 1440 <= r.end()) return true;
            }
            return false;
        }
    }

    // 자정부터 몇 분인지. 자정을 넘기면 end가 1440보다 큽니다 (17:00~02:00 → [1020, 1560]).
    public record Range(int start, int end) {}

    // Places API 응답 중 필요한 필드만
    record Details(RegularHours regularOpeningHours) {}
    record RegularHours(List<Period> periods) {}
    record Period(Point open, Point close) {}
    record Point(int day, int hour, int minute) {}  // day: 0=일요일 ~ 6=토요일

    private static final String DETAILS_URL = "https://places.googleapis.com/v1/places/{placeId}";

    private final String apiKey;
    private final RestClient rest = RestClient.create();

    public OpeningHours(@Value("${google.maps.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    // 영업시간 정보가 없는 장소(거리, 동네 등)는 null → 검증 생략.
    // API 오류(키, 할당량)는 예외로 던집니다.
    // ponytail: 기본 영업시간만 봄. 공휴일·임시 휴무(currentOpeningHours)는 7일 이내만 나와서 제외.
    public Hours get(String placeId) {
        Details d = rest.get().uri(DETAILS_URL, placeId)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "regularOpeningHours")
                .retrieve()
                .body(Details.class);
        if (d == null || d.regularOpeningHours() == null || d.regularOpeningHours().periods() == null) return null;
        return toHours(d.regularOpeningHours().periods());
    }

    static Hours toHours(List<Period> periods) {
        Map<DayOfWeek, List<Range>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) days.put(day, new ArrayList<>());

        // 24시간: 일요일 0:00 open 하나만 있고 close가 없음
        if (periods.size() == 1 && periods.get(0).close() == null) {
            days.values().forEach(l -> l.add(new Range(0, 1440)));
            return new Hours(true, days);
        }
        for (Period p : periods) {
            int start = p.open().hour() * 60 + p.open().minute();
            int daysLater = (p.close().day() - p.open().day() + 7) % 7;  // 자정 넘기면 1
            int end = daysLater * 1440 + p.close().hour() * 60 + p.close().minute();
            days.get(dayOf(p.open().day())).add(new Range(start, end));
        }
        return new Hours(false, days);
    }

    private static DayOfWeek dayOf(int googleDay) {
        return googleDay == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(googleDay);
    }
}
