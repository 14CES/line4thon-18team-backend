package com.dotlo.line4thon_18team.trip.repository;

import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 가장 최근에 손댄 작성 중 여행
    Optional<Trip> findFirstByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, TripStatus status);

    // 내 여행만 (남의 여행이면 비어 있음)
    Optional<Trip> findByIdAndUserId(Long id, Long userId);
}
