package com.dotlo.line4thon_18team.place;

import static com.dotlo.line4thon_18team.place.PlaceCategory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

// 실제 장소의 Places API primaryType·types 그대로 (2026-11-04 확인)
class PlaceCategoryMapperTest {

    private static void check(PlaceCategory expected, String name, String primary, String... types) {
        assertEquals(expected, PlaceCategoryMapper.of(primary, List.of(types)), name);
    }

    @Test
    void 실제_장소() {
        check(FOOD, "이치란", "ramen_restaurant",
                "ramen_restaurant", "chinese_noodle_restaurant", "noodle_shop", "snack_bar", "japanese_restaurant",
                "meal_takeaway", "restaurant", "food", "point_of_interest", "establishment");
        check(CAFE, "스타벅스", "coffee_shop", "coffee_shop", "cafe", "food_store", "food", "store");
        check(CAFE, "파블로 치즈타르트", "dessert_restaurant",
                "dessert_restaurant", "pastry_shop", "cake_shop", "dessert_shop", "bakery", "restaurant", "food");
        check(CULTURE, "오사카성", "castle", "castle", "tourist_attraction", "museum", "historical_place");
        check(CULTURE, "스미요시 대사", "shinto_shrine", "shinto_shrine", "tourist_attraction", "place_of_worship");
        check(CULTURE, "기요미즈데라", "buddhist_temple", "buddhist_temple", "tourist_attraction", "place_of_worship");
        check(CULTURE, "오사카 시립미술관", "art_museum", "art_museum", "museum");
        check(ACTIVITY, "해유관", "aquarium", "aquarium", "tourist_attraction");
        check(ACTIVITY, "USJ", "amusement_center", "tourist_attraction", "amusement_park", "amusement_center");
        check(ACTIVITY, "덴포잔 대관람차", "ferris_wheel", "ferris_wheel", "tourist_attraction");
        check(SHOPPING, "신사이바시스지 상점가", "market", "market", "tourist_attraction");
        check(SHOPPING, "한큐 백화점", "department_store", "department_store", "shopping_mall", "store");
        check(SHOPPING, "돈키호테", "discount_store", "discount_store", "general_store", "market", "store");
        check(SIGHTSEEING, "우메다 공중정원", "observation_deck", "observation_deck", "tourist_attraction");
        check(SIGHTSEEING, "츠텐카쿠", "observation_deck", "observation_deck", "landmark", "tourist_attraction");
        check(NATURE, "나카노시마 공원", "city_park", "city_park", "botanical_garden", "tourist_attraction", "park");
        check(NATURE, "아라시야마 대나무숲", "scenic_spot", "scenic_spot", "tourist_attraction", "park");
        check(NATURE, "나라공원", "park", "park", "tourist_attraction");
        check(NIGHTLIFE, "바 나유타", "cocktail_bar", "cocktail_bar", "bar");
        check(SIGHTSEEING, "신주쿠 골든가이", "tourist_attraction",
                "tourist_attraction", "ramen_restaurant", "japanese_restaurant", "bar", "restaurant", "food");
        check(SHOPPING, "쓰키지 장외시장", "wholesaler", "wholesaler", "service", "point_of_interest", "establishment");
    }

    @Test
    void 대표_type이_없을_때() {
        check(SIGHTSEEING, "도톤보리", null, "sublocality_level_2", "sublocality", "political");
        check(OTHER, "글리코 사인", null);
        assertEquals(OTHER, PlaceCategoryMapper.of(null, null));
        check(OTHER, "모르는 type", "car_repair", "car_repair", "point_of_interest", "establishment");
    }
}
