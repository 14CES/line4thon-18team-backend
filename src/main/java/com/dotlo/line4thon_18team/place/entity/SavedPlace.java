package com.dotlo.line4thon_18team.place.entity;

import com.dotlo.line4thon_18team.global.entity.BaseTimeEntity;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 여행에 저장된 장소 1개.
 * 같은 여행 안에서 같은 장소(feature ID)는 1행만 존재하고, 여러 리스트에 있었다면 listNames 에 쌓인다.
 */
@Entity
@Table(
        name = "saved_place",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saved_place_trip_feature",
                columnNames = {"trip_id", "feature_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    // 구글맵 URL 안의 "0x...:0x..." 값. 중복 판별 키. 직접 추가·AI 추천 장소는 없을 수 있다.
    @Column(name = "feature_id", length = 100)
    private String featureId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 2000)
    private String googleUrl;

    // enum 은 MySQL ENUM 대신 varchar 로 저장 (값이 추가돼도 컬럼을 고칠 필요가 없게)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private PlaceSource source;

    @ElementCollection
    @CollectionTable(name = "saved_place_list", joinColumns = @JoinColumn(name = "saved_place_id"))
    @Column(name = "list_name", nullable = false)
    private Set<String> listNames = new HashSet<>();

    // ---- BE2 매칭 후 채워지는 값 ----

    @Column(length = 100)
    private String placeId;

    @Column(length = 300)
    private String address;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private PlaceMatchStatus matchStatus;
}
