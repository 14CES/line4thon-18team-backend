package com.dotlo.line4thon_18team.member.repository;

import com.dotlo.line4thon_18team.member.entity.TripMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    boolean existsByTripIdAndUserId(Long tripId, Long userId);

    long countByTripId(Long tripId);

    // 구성원 목록 (참여한 순서). 이름/프로필을 같이 읽는다
    @EntityGraph(attributePaths = "user")
    List<TripMember> findAllByTripIdOrderByIdAsc(Long tripId);

    // 내가 참여한 여행들 (내 여행 목록에 같이 보여준다)
    @EntityGraph(attributePaths = "trip")
    List<TripMember> findAllByUserId(Long userId);
}
