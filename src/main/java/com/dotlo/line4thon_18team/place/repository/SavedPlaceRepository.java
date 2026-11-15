package com.dotlo.line4thon_18team.place.repository;

import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

    // listNames 를 같이 가져와서 장소마다 쿼리가 한 번씩 더 나가는 것(N+1)을 막는다
    @EntityGraph(attributePaths = "listNames")
    List<SavedPlace> findAllByTripIdOrderByIdAsc(Long tripId);

    List<SavedPlace> findAllByTripIdAndMatchStatus(Long tripId, PlaceMatchStatus matchStatus);

    Optional<SavedPlace> findFirstByTripIdAndPlaceId(Long tripId, String placeId);

    long countByTripId(Long tripId);

    void deleteAllByTripId(Long tripId);

    // 내가 만든 여행과 참여한 여행의 저장 장소 (찜 목록). 최근에 저장한 순
    // ponytail: 전부 한 번에 준다. 여행마다 수백 개씩 쌓여 느려지면 페이지로 나누기
    @EntityGraph(attributePaths = {"listNames", "trip"})
    @Query("select p from SavedPlace p where p.trip.user.id = :userId"
            + " or p.trip.id in (select m.trip.id from TripMember m where m.user.id = :userId)"
            + " order by p.id desc")
    List<SavedPlace> findAllInMyTrips(@Param("userId") Long userId);

    // 내가 만든 여행과 참여한 여행의 저장 장소 수 (마이)
    @Query("select count(p) from SavedPlace p where p.trip.user.id = :userId"
            + " or p.trip.id in (select m.trip.id from TripMember m where m.user.id = :userId)")
    long countInMyTrips(@Param("userId") Long userId);
}
