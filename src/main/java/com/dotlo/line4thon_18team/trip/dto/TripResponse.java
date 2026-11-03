package com.dotlo.line4thon_18team.trip.dto;

import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.entity.TripStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        Companion companion,
        int companionCount,
        TripStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getCity(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCompanion(),
                trip.getCompanionCount(),
                trip.getStatus(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }
}
