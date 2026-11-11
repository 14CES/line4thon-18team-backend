package com.dotlo.line4thon_18team.place.repository;

import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

    // listNames 를 같이 가져와서 장소마다 쿼리가 한 번씩 더 나가는 것(N+1)을 막는다
    @EntityGraph(attributePaths = "listNames")
    List<SavedPlace> findAllByTripIdOrderByIdAsc(Long tripId);

    List<SavedPlace> findAllByTripIdAndMatchStatus(Long tripId, PlaceMatchStatus matchStatus);

    Optional<SavedPlace> findFirstByTripIdAndPlaceId(Long tripId, String placeId);

    long countByTripId(Long tripId);
}
