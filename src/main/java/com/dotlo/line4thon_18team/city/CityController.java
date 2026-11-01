package com.dotlo.line4thon_18team.city;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CityController {

    /*
    - lat, lng: 도시 중심 좌표 (Places API "도시 이름" 검색 1위, 2026-11-02). 도시 밖 50km 판단 기준.
    - timezone: 영업시간·일정 시각은 이 시간대 기준입니다.
    */
    public record City(String name, String country, double lat, double lng, String timezone) {}

    // ponytail: 피그마 인기 여행지 6곳 고정. 도시 추가는 여기에 한 줄.
    static final List<City> CITIES = List.of(
            new City("오사카", "일본", 34.6952474, 135.5012079, "Asia/Tokyo"),
            new City("도쿄", "일본", 35.6764225, 139.6500270, "Asia/Tokyo"),
            new City("교토", "일본", 35.0115640, 135.7681489, "Asia/Tokyo"),
            new City("후쿠오카", "일본", 33.5901838, 130.4016888, "Asia/Tokyo"),
            new City("방콕", "태국", 13.7563309, 100.5017651, "Asia/Bangkok"),
            new City("다낭", "베트남", 16.0599691, 108.2111110, "Asia/Ho_Chi_Minh"));

    @GetMapping("/api/cities")
    public List<City> cities() {
        return CITIES;
    }
}
