package com.dotlo.line4thon_18team.itinerary.validation.result;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 하루 동안 일정을 넣을 수 있는 시간. 숙소에서 start에 출발해 end까지 숙소로 돌아오는 것을 기준으로 한다.
 *
 * @param adjustments 도착·출발 때문에 줄어든 경우 그 이유
 */
public record DayWindow(LocalDate date, LocalDateTime start, LocalDateTime end, Set<WindowAdjustment> adjustments) {

    public DayWindow {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("하루 끝이 시작보다 빠릅니다: " + start + " ~ " + end);
        }
        adjustments = adjustments == null || adjustments.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(adjustments));
    }

    /** 일정을 넣을 시간이 없는 날 (예: 밤늦게 도착한 첫날). */
    public boolean isEmpty() {
        return !start.isBefore(end);
    }

    public int availableMinutes() {
        return isEmpty() ? 0 : (int) Duration.between(start, end).toMinutes();
    }
}
