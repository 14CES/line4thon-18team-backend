package com.dotlo.line4thon_18team.place;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
    - category, closed: place의 장소 유형, 폐업 여부. MATCHED일 때만 값이 있습니다.
    */
    public record MatchResult(Long id, MatchStatus status, Candidate place, List<Candidate> candidates,
                              PlaceCategory category, boolean closed) {}

    // category: 장소 유형 (FE 검색·근처 추천 목록의 "카페 | 도톤보리에서 200m"). 구글 결과가 아니면 null
    public record Candidate(String placeId, String name, String address, double lat, double lng,
                            PlaceCategory category) {
        public Candidate(String placeId, String name, String address, double lat, double lng) {
            this(placeId, name, address, lat, lng, null);
        }
    }

    public enum MatchStatus {
        MATCHED,       // 확정
        NEEDS_REVIEW,  // 사용자 확인 필요 (candidates 중 선택)
        NOT_FOUND      // 검색 결과 없음
    }

    public record LatLng(double lat, double lng) {}

    // 후보 + 비교용 CID, 유형, 폐업 여부 (후보 목록에는 안 나감)
    private record Hit(Candidate candidate, String cid, PlaceCategory category, boolean closed) {}

    // Places API 응답 중 필요한 필드만
    record SearchResponse(List<Place> places) {}
    record Place(String id, Text displayName, String formattedAddress, Location location, String googleMapsUri,
                 String primaryType, List<String> types, String businessStatus) {}
    record Text(String text) {}
    record Location(double latitude, double longitude) {}

    private static final String SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String NEARBY_URL = "https://places.googleapis.com/v1/places:searchNearby";  // 같은 필드라 Nearby Search Pro 요금
    private static final int NEARBY_RADIUS_M = 2000;
    // 근처 추천 탭마다 구글에 넘기는 장소 유형. 구글은 보조 유형까지 맞춰서 백화점이 맛집에 섞이므로 결과를 우리 분류로 한 번 더 거름
    // (2026-11-16 오사카 도톤보리 기준 확인)
    private static final Map<PlaceCategory, List<String>> NEARBY_TYPES = Map.of(
            PlaceCategory.FOOD, List.of("restaurant"),
            PlaceCategory.CAFE, List.of("cafe", "coffee_shop", "bakery", "dessert_shop"),
            PlaceCategory.SIGHTSEEING, List.of("tourist_attraction", "observation_deck"),
            PlaceCategory.SHOPPING, List.of("shopping_mall", "department_store", "market"),
            PlaceCategory.NATURE, List.of("park", "garden", "beach", "hiking_area"),
            PlaceCategory.CULTURE, List.of("museum", "art_gallery", "historical_landmark", "cultural_landmark",
                    "shinto_shrine", "buddhist_temple", "castle"),
            PlaceCategory.NIGHTLIFE, List.of("bar", "night_club"),
            PlaceCategory.ACTIVITY, List.of("amusement_park", "aquarium", "zoo"));
    private static final String FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,places.googleMapsUri,"
                    + "places.primaryType,places.types,places.businessStatus";  // 전부 Text Search Pro 요금
    private static final String DETAILS_URL = "https://places.googleapis.com/v1/places/{placeId}?languageCode=ko";
    private static final String DETAILS_FIELD_MASK =
            "id,displayName,formattedAddress,location,primaryType,types,businessStatus";  // 전부 Place Details Pro 요금
    private static final int CITY_RADIUS_M = 15000;
    private static final int PLACE_RADIUS_M = 200;
    private static final int MAX_DIST_M = 50;
    private static final Pattern CID_PARAM = Pattern.compile("[?&]cid=(\\d+)");
    private static final Pattern COORDINATES = Pattern.compile("\\s*-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?\\s*");

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
        List<Hit> hits = search(cityName, null, null, 0, 1, false);
        if (hits.isEmpty()) throw new BusinessException(ErrorCode.CITY_NOT_FOUND, "도시를 찾을 수 없습니다: " + cityName);
        Candidate c = hits.get(0).candidate();
        return new LatLng(c.lat(), c.lng());
    }

    // 장소 검색 (장소 직접 추가, BE3 AI 장소 추가). 중심에서 radius 미터 안의 결과만, 최대 10개.
    // 결과가 없으면 빈 리스트 → 호출하는 쪽에서 radius를 넓혀 다시 검색.
    public List<Candidate> searchNear(String query, double lat, double lng, int radius) {
        return search(query, lat, lng, radius, 10, true).stream()
                .map(Hit::candidate)
                .filter(c -> meters(lat, lng, c.lat(), c.lng()) <= radius)  // 검색 범위가 사각형이라 모서리 부분은 직접 거름
                .toList();
    }

    // 근처 추천. 중심에서 2km 안 구글 인기 장소 20곳 중 이 유형만, 가까운 순. category가 null이면 모든 유형 (추천 탭)
    // 호텔은 숙소 등록에서 따로 고르므로 뺍니다. category는 OTHER가 아니어야 합니다.
    public List<Candidate> searchNearby(double lat, double lng, PlaceCategory category) {
        List<String> types = category == null
                ? NEARBY_TYPES.values().stream().flatMap(List::stream).toList()
                : NEARBY_TYPES.get(category);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("includedTypes", types);
        body.put("excludedPrimaryTypes", List.of("hotel", "lodging"));
        body.put("maxResultCount", 20);
        body.put("rankPreference", "POPULARITY");
        body.put("languageCode", "ko");
        body.put("locationRestriction", Map.of("circle", Map.of(
                "center", Map.of("latitude", lat, "longitude", lng),
                "radius", NEARBY_RADIUS_M)));
        return nearest(post(NEARBY_URL, body), lat, lng, category);
    }

    // 폐업·다른 유형을 빼고 가까운 순
    static List<Candidate> nearest(List<Place> places, double lat, double lng, PlaceCategory category) {
        return places.stream()
                .map(PlaceMatcher::toHit)
                .filter(h -> !h.closed())
                .filter(h -> category == null ? h.category() != PlaceCategory.OTHER : h.category() == category)
                .map(Hit::candidate)
                .sorted(Comparator.comparingDouble(c -> meters(lat, lng, c.lat(), c.lng())))
                .toList();
    }

    // 사용자가 고른 장소(placeId)의 정보. 결과는 MATCHED (폐업·도시 밖 판단은 받는 쪽에서)
    public MatchResult details(Long id, String placeId) {
        Place pl;
        try {
            pl = rest.get().uri(DETAILS_URL, placeId)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
                    .retrieve()
                    .body(Place.class);
        } catch (HttpClientErrorException.BadRequest e) {
            // 키가 틀려도 400이라 메시지로 구분: "The provided Place ID: ... is not valid." (2026-11-05 확인)
            if (e.getResponseBodyAsString().contains("Place ID")) throw new BusinessException(ErrorCode.INVALID_PLACE_ID);
            throw e;
        }
        return matched(new PlaceInput(id, null, null, null, null), toHit(pl));
    }

    // Takeout 에서 이름 없는 핀은 이름이 "34.6687, 135.5013" 처럼 좌표로 들어온다 (TakeoutParser)
    public static boolean isCoordinates(String name) {
        return name != null && COORDINATES.matcher(name).matches();
    }

    private MatchResult match(PlaceInput p, double cityLat, double cityLng) {
        // 구글 검색은 검색어가 좌표면 400 오류를 냄 (2026-11-05 확인). 검색하지 않고 사용자가 후보를 고르게 함
        if (isCoordinates(p.name())) return new MatchResult(p.id(), MatchStatus.NEEDS_REVIEW, null, List.of(), null, false);
        boolean hasCoord = p.lat() != null && p.lng() != null;
        // 좌표 있으면 그 자리 200m, 없으면 도시 중심 15km
        // 우선(bias)만 걸어서 도시 밖 장소도 찾음. 제한(restriction)이면 교토 기요미즈데라가 오사카 덴노지 기요미즈데라로 바뀜 (2026-11-04 확인)
        List<Hit> hits = hasCoord
                ? search(p.name(), p.lat(), p.lng(), PLACE_RADIUS_M, 10, false)
                : search(p.name(), cityLat, cityLng, CITY_RADIUS_M, 10, false);
        if (hits.isEmpty()) return new MatchResult(p.id(), MatchStatus.NOT_FOUND, null, List.of(), null, false);

        // 1) CID 일치
        String cid = cidOf(p.featureId());
        if (cid != null) {
            for (Hit h : hits) {
                if (cid.equals(h.cid())) return matched(p, h);
            }
        }
        // 2) 좌표 있으면 1위가 50m 이내인지
        Hit top = hits.get(0);
        if (hasCoord && meters(p.lat(), p.lng(), top.candidate().lat(), top.candidate().lng()) <= MAX_DIST_M) {
            return matched(p, top);
        }

        return new MatchResult(p.id(), MatchStatus.NEEDS_REVIEW, null,
                hits.stream().map(Hit::candidate).toList(), null, false);
    }

    private static MatchResult matched(PlaceInput p, Hit h) {
        return new MatchResult(p.id(), MatchStatus.MATCHED, h.candidate(), List.of(), h.category(), h.closed());
    }

    // "0x6000e691ef37d6b7:0xb291250c3595327a" → "12867106344743023226" (long 범위를 넘어서 unsigned로 처리)
    static String cidOf(String featureId) {
        if (featureId == null) return null;
        int i = featureId.indexOf(":0x");
        if (i < 0) return null;
        return Long.toUnsignedString(Long.parseUnsignedLong(featureId.substring(i + 3), 16));
    }

    // restrict: true면 범위 밖 결과를 뺌(검색 API), false면 범위 안을 우선만 함(매칭)
    private List<Hit> search(String query, Double lat, Double lng, int radius, int size, boolean restrict) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", query);
        body.put("languageCode", "ko");
        body.put("pageSize", size);
        if (lat != null && !restrict) {
            body.put("locationBias", Map.of("circle", Map.of(
                    "center", Map.of("latitude", lat, "longitude", lng),
                    "radius", radius)));
        } else if (lat != null) {
            // 검색 API는 제한(restriction). 우선(bias)이면 "그레이스리 호텔"을 오사카에서 찾아도 서울 지점만 나옴 (2026-11-03 확인)
            // restriction은 사각형만 지원해서 반경을 감싸는 사각형으로 보냄
            double dLat = radius / 111_320.0;
            double dLng = radius / (111_320.0 * Math.cos(Math.toRadians(lat)));
            body.put("locationRestriction", Map.of("rectangle", Map.of(
                    "low", Map.of("latitude", lat - dLat, "longitude", lng - dLng),
                    "high", Map.of("latitude", lat + dLat, "longitude", lng + dLng))));
        }
        return post(SEARCH_URL, body).stream().map(PlaceMatcher::toHit).toList();
    }

    private List<Place> post(String url, Map<String, Object> body) {
        SearchResponse res = rest.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .body(body)
                .retrieve()
                .body(SearchResponse.class);
        return res == null || res.places() == null ? List.of() : res.places();  // 결과 0개면 {} 가 옴
    }

    private static Hit toHit(Place pl) {
        Matcher m = CID_PARAM.matcher(pl.googleMapsUri() == null ? "" : pl.googleMapsUri());
        PlaceCategory category = PlaceCategoryMapper.of(pl.primaryType(), pl.types());
        return new Hit(new Candidate(pl.id(), pl.displayName().text(), pl.formattedAddress(),
                pl.location().latitude(), pl.location().longitude(), category),
                m.find() ? m.group(1) : null,
                category,
                "CLOSED_PERMANENTLY".equals(pl.businessStatus()));
    }

    public static double meters(double lat1, double lng1, double lat2, double lng2) {
        double r1 = Math.toRadians(lat1), r2 = Math.toRadians(lat2);
        double h = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(r1) * Math.cos(r2) * Math.pow(Math.sin(Math.toRadians(lng2 - lng1) / 2), 2);
        return 2 * 6371000 * Math.asin(Math.sqrt(h));
    }
}
