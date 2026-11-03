package com.dotlo.line4thon_18team.place.dto;

import com.dotlo.line4thon_18team.place.entity.PlaceMatchStatus;
import com.dotlo.line4thon_18team.place.entity.PlaceSource;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;

import java.util.List;

public record SavedPlaceResponse(
        Long id,
        String featureId,
        String name,
        String note,
        String googleUrl,
        PlaceSource source,
        List<String> listNames,
        // 아래는 BE2 매칭 후 채워지는 값 (매칭 전엔 null / PENDING)
        String placeId,
        String address,
        Double latitude,
        Double longitude,
        PlaceMatchStatus matchStatus
) {

    public static SavedPlaceResponse from(SavedPlace place) {
        return new SavedPlaceResponse(
                place.getId(),
                place.getFeatureId(),
                place.getName(),
                place.getNote(),
                place.getGoogleUrl(),
                place.getSource(),
                place.getListNames().stream().sorted().toList(),
                place.getPlaceId(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getMatchStatus()
        );
    }
}
