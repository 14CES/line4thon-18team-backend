package com.dotlo.line4thon_18team.itinerary.validation.result;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 계산된 하루 일정.
 *
 * @param window    그날 쓸 수 있는 시간
 * @param visits    방문 순서대로 계산한 시각
 * @param returnLeg 마지막 장소에서 숙소로 돌아가는 이동 (방문이 없으면 null)
 * @param endsAt    숙소 도착 시각 (방문이 없으면 window.start)
 */
public record DayTimeline(LocalDate date,
                          DayWindow window,
                          List<ScheduledVisit> visits,
                          TravelLeg returnLeg,
                          LocalDateTime endsAt) {

    public DayTimeline {
        visits = visits == null ? List.of() : List.copyOf(visits);
    }
}
