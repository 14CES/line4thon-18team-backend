package com.dotlo.line4thon_18team.preference;

import com.dotlo.line4thon_18team.preference.domain.PreferenceChip;
import com.dotlo.line4thon_18team.preference.domain.PreferenceChips;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;

import java.time.Instant;
import java.util.List;

/**
 * 취향 저장·조회 응답.
 *
 * @param saved 저장된 값이 있으면 true. false면 기본값(보통)을 돌려준 것.
 */
public record PreferenceView(long tripId,
                             long userId,
                             boolean saved,
                             PreferenceProfile preferences,
                             List<PreferenceChip> chips,
                             String rawText,
                             Instant updatedAt) {

    public PreferenceView {
        chips = chips == null ? List.of() : List.copyOf(chips);
    }

    static PreferenceView of(StoredPreference stored) {
        return new PreferenceView(stored.tripId(), stored.userId(), true, stored.profile(),
                PreferenceChips.all(stored.profile()), stored.rawText(), stored.updatedAt());
    }

    static PreferenceView defaults(long tripId, long userId) {
        PreferenceProfile defaults = PreferenceProfile.defaults();
        return new PreferenceView(tripId, userId, false, defaults, PreferenceChips.all(defaults), null, null);
    }
}
