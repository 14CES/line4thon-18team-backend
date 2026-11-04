package com.dotlo.line4thon_18team.place.controller;

import com.dotlo.line4thon_18team.auth.LoginUser;
import com.dotlo.line4thon_18team.place.dto.SavedPlaceListResponse;
import com.dotlo.line4thon_18team.place.dto.TakeoutUploadResponse;
import com.dotlo.line4thon_18team.place.service.SavedPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    // Takeout CSV 하나 또는 Takeout zip 업로드 (form-data, key: file)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TakeoutUploadResponse> uploadTakeout(@LoginUser Long userId, @PathVariable Long tripId,
                                                               @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(savedPlaceService.uploadTakeout(userId, tripId, file));
    }

    // 저장 장소 삭제 → 204 No Content
    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> deletePlace(@LoginUser Long userId, @PathVariable Long tripId,
                                            @PathVariable Long placeId) {
        savedPlaceService.deletePlace(userId, tripId, placeId);
        return ResponseEntity.noContent().build();
    }
}
