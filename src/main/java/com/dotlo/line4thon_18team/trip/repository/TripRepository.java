package com.dotlo.line4thon_18team.trip.repository;

import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.entity.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 가장 최근에 손댄 작성 중 여행
    Optional<Trip> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, TripStatus status);

    // 내 여행만 (남의 여행이면 비어 있음)
    Optional<Trip> findByIdAndUserId(Long id, Long userId);

    // 여행 행 잠금 (SELECT ... FOR UPDATE). 같은 여행을 잠그려는 다른 트랜잭션은 이 트랜잭션이 끝날 때까지 기다린다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
