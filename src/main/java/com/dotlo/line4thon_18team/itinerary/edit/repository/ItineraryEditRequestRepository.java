package com.dotlo.line4thon_18team.itinerary.edit.repository;

import com.dotlo.line4thon_18team.itinerary.edit.entity.ItineraryEditRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItineraryEditRequestRepository extends JpaRepository<ItineraryEditRequest, Long> {

    Optional<ItineraryEditRequest> findByIdAndTripIdAndCreatedBy(Long id, Long tripId, Long createdBy);

    /** 구버전 적용 요청에서 본인의 수정안을 하나로 특정할 수 있는지 확인한다. */
    List<ItineraryEditRequest> findTop2ByTripIdAndBaseVersionAndCreatedBy(Long tripId, int baseVersion, Long createdBy);

    /** 여행 삭제용. */
    void deleteByTripId(Long tripId);
}
