package com.dotlo.line4thon_18team.place.dto;

import jakarta.validation.constraints.NotBlank;

// 구글맵 공유 리스트 링크 (예: https://maps.app.goo.gl/...)
public record PlaceLinkRequest(
        @NotBlank(message = "공유 리스트 링크가 필요합니다.")
        String url
) {
}
