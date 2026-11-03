package com.dotlo.line4thon_18team.place.controller;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.place.dto.SavedPlaceListResponse;
import com.dotlo.line4thon_18team.place.service.SavedPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips/{tripId}/places")
public class SavedPlaceController {

    private final SavedPlaceService savedPlaceService;

    // 이 여행에 저장된 장소 목록
    @GetMapping
    public ResponseEntity<SavedPlaceListResponse> getPlaces(@LoginUser Long userId, @PathVariable Long tripId) {
        return ResponseEntity.ok(savedPlaceService.getPlaces(userId, tripId));
    }
}
