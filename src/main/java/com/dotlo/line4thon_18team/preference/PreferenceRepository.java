package com.dotlo.line4thon_18team.preference;

import java.util.List;
import java.util.Optional;

/**
 * 취향 저장소. 지금은 메모리 구현만 있다.
 * BE1 DB 구조가 정해지면 JPA 등으로 이 인터페이스를 구현하면 된다.
 */
public interface PreferenceRepository {

    /** 같은 (tripId, userId)가 있으면 덮어쓴다. */
    StoredPreference save(StoredPreference preference);

    Optional<StoredPreference> findByTripAndUser(long tripId, long userId);

    /** 여행 구성원 전체의 취향 (동행자 기능 P2에서 사용). userId 오름차순. */
    List<StoredPreference> findAllByTrip(long tripId);
}
