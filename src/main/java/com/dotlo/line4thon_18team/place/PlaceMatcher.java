package com.dotlo.line4thon_18team.place;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** BE2 장소 매칭. BE1은 이 서비스를 주입받아 직접 호출합니다. */
@Service
public class PlaceMatcher {

    /*
    - id: BE1 쪽 행 번호. 결과를 다시 연결할 때 씁니다.
    - name: 검색어로 씁니다.
    - featureId: "0x...:0x..." 형태. 콜론 뒤가 CID(16진수)입니다. 없으면 null.
    - lat, lng: 공유 URL에서 온 장소만 값이 있고, Takeout에서 온 장소는 null입니다.
    */
    public record PlaceInput(Long id, String name, String featureId, Double lat, Double lng) {}

    /*
    - place: 확정된 장소. MATCHED일 때만 값이 있고 나머지는 null입니다.
    - candidates: NEEDS_REVIEW일 때 사용자에게 보여 줄 후보 목록. 나머지는 빈 리스트입니다.
    */
    public record MatchResult(Long id, MatchStatus status, Candidate place, List<Candidate> candidates) {}

    public record Candidate(String placeId, String name, String address, double lat, double lng) {}

    public enum MatchStatus {
        MATCHED,       // 확정
        NEEDS_REVIEW,  // 사용자 확인 필요 (candidates 중 선택)
        NOT_FOUND      // 검색 결과 없음
    }

    public record LatLng(double lat, double lng) {}

    // 후보 + 비교용 CID (CID는 출력에 안 나감)
    private record Hit(Candidate candidate, String cid) {}

    // Places API 응답 중 필요한 필드만
    record SearchResponse(List<Place> places) {}
    record Place(String id, Text displayName, String formattedAddress, Location location, String googleMapsUri) {}
    record Text(String text) {}
    record Location(double latitude, double longitude) {}

    private static final String SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.googleMapsUri";
    private static final int CITY_RADIUS_M = 15000;
    private static final int PLACE_RADIUS_M = 200;
    private static final int MAX_DIST_M = 50;
    private static final Pattern CID_PARAM = Pattern.compile("[?&]cid=(\\d+)");

    private final String apiKey;
    private final RestClient rest = GoogleClient.create();

    public PlaceMatcher(@Value("${google.maps.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    // API 오류(키, 할당량)는 예외로 던집니다.
    public List<MatchResult> matchPlaces(List<PlaceInput> places, double cityLat, double cityLng) {
        return places.stream().map(p -> match(p, cityLat, cityLng)).toList();
    }

    // 여행 생성 시 1회. 못 찾으면 예외.
    public LatLng findCityLocation(String cityName) {
        List<Hit> hits = search(cityName, null, null, 0, 1);
        if (hits.isEmpty()) throw new BusinessException(ErrorCode.CITY_NOT_FOUND, "도시를 찾을 수 없습니다: " + cityName);
        Candidate c = hits.get(0).candidate();
        return new LatLng(c.lat(), c.lng());
    }

    // 장소 검색 (장소 직접 추가, BE3 AI 장소 추가). 중심에서 radius 미터 안의 결과만, 최대 10개.
    // 결과가 없으면 빈 리스트 → 호출하는 쪽에서 radius를 넓혀 다시 검색.
    public List<Candidate> searchNear(String query, double lat, double lng, int radius) {
        return search(query, lat, lng, radius, 10).stream()
                .map(Hit::candidate)
                .filter(c -> meters(lat, lng, c.lat(), c.lng()) <= radius)  // 검색 범위는 필터가 아니라서 직접 거름
                .toList();
    }

    private MatchResult match(PlaceInput p, double cityLat, double cityLng) {
        boolean hasCoord = p.lat() != null && p.lng() != null;
        // 좌표 있으면 그 자리 200m, 없으면 도시 중심 15km
        List<Hit> hits = hasCoord
                ? search(p.name(), p.lat(), p.lng(), PLACE_RADIUS_M, 10)
                : search(p.name(), cityLat, cityLng, CITY_RADIUS_M, 10);
        if (hits.isEmpty()) return new MatchResult(p.id(), MatchStatus.NOT_FOUND, null, List.of());

        // 1) CID 일치
        String cid = cidOf(p.featureId());
        if (cid != null) {
            for (Hit h : hits) {
                if (cid.equals(h.cid())) return matched(p, h.candidate());
            }
        }
        // 2) 좌표 있으면 1위가 50m 이내인지
        Candidate top = hits.get(0).candidate();
        if (hasCoord && meters(p.lat(), p.lng(), top.lat(), top.lng()) <= MAX_DIST_M) return matched(p, top);

        return new MatchResult(p.id(), MatchStatus.NEEDS_REVIEW, null,
                hits.stream().map(Hit::candidate).toList());
    }

    private static MatchResult matched(PlaceInput p, Candidate c) {
        return new MatchResult(p.id(), MatchStatus.MATCHED, c, List.of());
    }

    // "0x6000e691ef37d6b7:0xb291250c3595327a" → "12867106344743023226" (long 범위를 넘어서 unsigned로 처리)
    static String cidOf(String featureId) {
        if (featureId == null) return null;
        int i = featureId.indexOf(":0x");
        if (i < 0) return null;
        return Long.toUnsignedString(Long.parseUnsignedLong(featureId.substring(i + 3), 16));
    }

    private List<Hit> search(String query, Double lat, Double lng, int radius, int size) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", query);
        body.put("languageCode", "ko");
        body.put("pageSize", size);
        if (lat != null) {
            body.put("locationBias", Map.of("circle", Map.of(
                    "center", Map.of("latitude", lat, "longitude", lng),
                    "radius", radius)));
        }
        SearchResponse res = rest.post().uri(SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(body)
                .retrieve()
                .body(SearchResponse.class);
        if (res == null || res.places() == null) return List.of();  // 결과 0개면 {} 가 옴

        return res.places().stream().map(pl -> {
            Matcher m = CID_PARAM.matcher(pl.googleMapsUri() == null ? "" : pl.googleMapsUri());
            return new Hit(new Candidate(pl.id(), pl.displayName().text(), pl.formattedAddress(),
                    pl.location().latitude(), pl.location().longitude()),
                    m.find() ? m.group(1) : null);
        }).toList();
    }

    static double meters(double lat1, double lng1, double lat2, double lng2) {
        double r1 = Math.toRadians(lat1), r2 = Math.toRadians(lat2);
        double h = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(r1) * Math.cos(r2) * Math.pow(Math.sin(Math.toRadians(lng2 - lng1) / 2), 2);
        return 2 * 6371000 * Math.asin(Math.sqrt(h));
    }
}
