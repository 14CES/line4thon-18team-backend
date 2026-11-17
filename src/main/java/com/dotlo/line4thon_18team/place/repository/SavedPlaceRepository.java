package com.dotlo.line4thon_18team.place.repository;

import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    /** 장소별로 담은 여행 수. */
    record PlaceCount(String placeId, long trips) {
    }

    // 도시에서 많은 여행이 담은 장소 순 (같으면 최근에 담은 순). 이 여행에 이미 있는 장소는 뺀다
    // ponytail: 요청마다 도시의 저장 장소를 모아 센다. 저장 장소가 수십만 행이 되면 집계 테이블로
    @Query("""
            select new com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository$PlaceCount(
                p.placeId, count(distinct p.trip.id))
            from SavedPlace p
            where p.trip.city = :city and p.matchStatus = :status and p.placeId is not null
              and p.placeId not in (
                  select q.placeId from SavedPlace q where q.trip.id = :tripId and q.placeId is not null)
            group by p.placeId
            order by count(distinct p.trip.id) desc, max(p.id) desc
            """)
    List<PlaceCount> countPopularInCity(@Param("city") String city, @Param("tripId") Long tripId,
                                        @Param("status") PlaceMatchStatus status, Pageable pageable);

    List<SavedPlace> findAllByPlaceIdInAndMatchStatusOrderByIdDesc(Collection<String> placeIds,
                                                                   PlaceMatchStatus matchStatus);
}
