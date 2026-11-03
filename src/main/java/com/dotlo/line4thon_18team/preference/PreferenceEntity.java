package com.dotlo.line4thon_18team.preference;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.preference.domain.DayStartPreference;
import com.dotlo.line4thon_18team.preference.domain.PreferenceProfile;
import com.dotlo.line4thon_18team.preference.domain.TravelPace;
import com.dotlo.line4thon_18team.preference.domain.WalkingPreference;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 취향 테이블. 여행 + 사용자마다 한 줄 (동행자 기능에서 구성원별 취향이 필요해서).
 *
 * <p>여행 테이블(BE1)이 아직 없어서 trip_id는 FK 없이 숫자로만 둔다.
 * updatedAt은 응답에 서비스 시계(Clock) 기준 시각을 그대로 쓰려고 BaseTimeEntity 대신 직접 둔다.
 */
@Entity
@Table(
        name = "trip_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uk_trip_preferences_trip_user", columnNames = {"trip_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelPace pace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalkingPreference walking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayStartPreference dayStart;

    // 장소 유형 이름 목록. 예) ["CAFE","FOOD"]
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 200)
    private List<String> interests;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 200)
    private List<String> avoid;

    // 최대 5개, 각 100자 (PreferenceRules)
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 1000)
    private List<String> notes;

    // 사용자가 입력한 취향 원문 (최대 500자)
    @Column(length = 500)
    private String rawText;

    @Column(nullable = false)
    private Instant updatedAt;

    PreferenceEntity(long tripId, long userId) {
        this.tripId = tripId;
        this.userId = userId;
    }

    void update(PreferenceProfile profile, String rawText, Instant updatedAt) {
        this.pace = profile.pace();
        this.walking = profile.walking();
        this.dayStart = profile.dayStart();
        this.interests = profile.interests().stream().map(Enum::name).toList();
        this.avoid = profile.avoid().stream().map(Enum::name).toList();
        this.notes = profile.notes();
        this.rawText = rawText;
        this.updatedAt = updatedAt;
    }

    StoredPreference toStored() {
        PreferenceProfile profile = new PreferenceProfile(pace, walking, dayStart,
                categories(interests), categories(avoid), notes);
        return new StoredPreference(tripId, userId, profile, rawText, updatedAt);
    }

    private static List<PlaceCategory> categories(List<String> names) {
        return names.stream().map(PlaceCategory::valueOf).toList();
    }
}
