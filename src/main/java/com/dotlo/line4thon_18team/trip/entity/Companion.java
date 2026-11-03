package com.dotlo.line4thon_18team.trip.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 누구와 떠나는지. 인원은 나를 포함한 수.
 * 혼자·연인은 인원이 정해져 있고, 친구·가족은 2~10명 중 고른다.
 */
@Getter
@RequiredArgsConstructor
public enum Companion {
    ALONE(1, 1),
    COUPLE(2, 2),
    FRIEND(2, 10),
    FAMILY(2, 10);

    private final int minCount;
    private final int maxCount;

    public boolean isFixedCount() {
        return minCount == maxCount;
    }

    public boolean allows(int count) {
        return minCount <= count && count <= maxCount;
    }
}
