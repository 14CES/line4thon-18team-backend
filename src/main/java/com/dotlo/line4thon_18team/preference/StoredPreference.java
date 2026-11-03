package com.dotlo.line4thon_18team.preference;

import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;

import java.time.Instant;
import java.util.Objects;

/**
 * 저장된 취향 한 건. 여행 + 사용자 단위로 저장한다 (동행자 기능에서 구성원별 취향이 필요해서).
 * DB에는 PreferenceEntity(trip_preferences 테이블)로 저장한다.
 */
public record StoredPreference(long tripId, long userId, PreferenceProfile profile, String rawText, Instant updatedAt) {

    public StoredPreference {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
