package com.dotlo.line4thon_18team.place.controller;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.place.dto.MySavedPlaceResponse;
import com.dotlo.line4thon_18team.place.service.SavedPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MySavedPlaceController {

    private final SavedPlaceService savedPlaceService;

    // 장소 찜 목록: 내가 만든 여행과 참여한 여행에 저장한 장소 모두 (최근에 저장한 순)
    @GetMapping("/api/users/me/places")
    public List<MySavedPlaceResponse> myPlaces(@LoginUser Long userId) {
        return savedPlaceService.getMyPlaces(userId);
    }
}
