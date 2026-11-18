package com.dotlo.line4thon_18team.place;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** BE2 장소 상세 정보 (평점·리뷰 수·소개). 영업시간은 OpeningHours. */
@Service
public class PlaceDetails {

    /*
    - rating, reviewCount: 리뷰가 없는 장소는 null
    - summary: 구글 한 줄 소개 (한국어). 없는 장소도 있어서 null 가능 (2026-11-17 하브스 난바파크스점은 없음)
    - address: 일본 주소는 languageCode=ko 여도 영어로 옴
    */
    public record Detail(String placeId, String name, PlaceCategory category, Double rating, Integer reviewCount,
                         String summary, String address, double lat, double lng, String googleMapsUri) {}

    // Places API 응답 중 필요한 필드만
    record Place(String id, Text displayName, String formattedAddress, Location location, String googleMapsUri,
                 String primaryType, List<String> types, Double rating, Integer userRatingCount,
                 Text editorialSummary) {}
    record Text(String text) {}
    record Location(double latitude, double longitude) {}

    private static final String DETAILS_URL = "https://places.googleapis.com/v1/places/{placeId}?languageCode=ko";
    // editorialSummary 때문에 Place Details Enterprise + Atmosphere 요금
    private static final String FIELD_MASK = "id,displayName,formattedAddress,location,googleMapsUri,primaryType,types,"
            + "rating,userRatingCount,editorialSummary";

    private final String apiKey;
    private final RestClient rest = GoogleClient.create();

    public PlaceDetails(@Value("${google.maps.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    // 없는 장소 ID는 400 INVALID_PLACE_ID. 그 밖의 API 오류(키, 할당량)는 예외로 던집니다 (OpeningHours와 같음)
    public Detail get(String placeId) {
        Place p;
        try {
            p = rest.get().uri(DETAILS_URL, placeId)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .retrieve()
                    .body(Place.class);
        } catch (HttpClientErrorException.BadRequest e) {
            if (e.getResponseBodyAsString().contains("Place ID")) throw new BusinessException(ErrorCode.INVALID_PLACE_ID);
            throw e;
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(ErrorCode.INVALID_PLACE_ID);
        }
        return new Detail(p.id(), text(p.displayName()), PlaceCategoryMapper.of(p.primaryType(), p.types()),
                p.rating(), p.userRatingCount(), text(p.editorialSummary()), p.formattedAddress(),
                p.location().latitude(), p.location().longitude(), p.googleMapsUri());
    }

    private static String text(Text t) {
        return t == null ? null : t.text();
    }
}
