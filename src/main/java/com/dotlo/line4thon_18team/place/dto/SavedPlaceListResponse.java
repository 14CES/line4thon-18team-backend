package com.dotlo.line4thon_18team.place.dto;

import java.util.List;

public record SavedPlaceListResponse(
        Long tripId,
        int count,
        List<SavedPlaceResponse> places
) {
}
