package com.dotlo.line4thon_18team.place;

import static com.dotlo.line4thon_18team.place.PlaceCategory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Places API 장소 type → PlaceCategory. 장소 보강 때 씁니다.
 * 대표 type(primaryType)부터 보고, 없으면 types를 앞에서부터 봅니다. 아무것도 안 맞으면 OTHER.
 */
public final class PlaceCategoryMapper {

    // 2026-11-04 오사카·교토·도쿄 등 실제 장소 type으로 확인 (PlaceCategoryMapperTest)
    private static final Map<String, PlaceCategory> EXACT = new HashMap<>();
    static {
        put(FOOD, "restaurant", "food_court", "meal_takeaway", "meal_delivery", "deli", "diner", "noodle_shop",
                "sandwich_shop", "cafeteria", "bar_and_grill", "steak_house");
        // dessert_restaurant는 "_restaurant"지만 카페 (예: 파블로 치즈타르트)
        put(CAFE, "cafe", "coffee_shop", "bakery", "dessert_shop", "dessert_restaurant", "tea_house", "ice_cream_shop",
                "donut_shop", "pastry_shop", "cake_shop", "juice_shop", "chocolate_shop", "confectionery", "cat_cafe",
                "dog_cafe");
        put(SIGHTSEEING, "observation_deck", "landmark", "plaza", "bridge");
        put(SHOPPING, "shopping_mall", "department_store", "market", "gift_shop", "wholesaler");  // wholesaler: 쓰키지 장외시장
        put(NATURE, "park", "city_park", "national_park", "state_park", "garden", "botanical_garden", "beach",
                "hiking_area", "scenic_spot", "nature_preserve", "mountain_peak", "lake", "river", "island", "woods",
                "wildlife_park");
        put(CULTURE, "museum", "art_gallery", "castle", "historical_place", "historical_landmark", "cultural_landmark",
                "monument", "shinto_shrine", "buddhist_temple", "place_of_worship", "church", "hindu_temple", "mosque",
                "synagogue", "performing_arts_theater", "opera_house", "concert_hall", "cultural_center");
        put(NIGHTLIFE, "bar", "pub", "night_club", "wine_bar", "cocktail_bar", "lounge_bar", "sports_bar", "irish_pub",
                "brewpub", "beer_hall", "karaoke");
        put(ACTIVITY, "amusement_park", "amusement_center", "water_park", "aquarium", "zoo", "ferris_wheel",
                "roller_coaster", "bowling_alley", "video_arcade", "planetarium", "movie_theater", "casino",
                "ski_resort", "golf_course", "adventure_sports_center", "go_karting_venue", "spa", "public_bath",
                "sauna");
    }

    private PlaceCategoryMapper() {}

    public static PlaceCategory of(String primaryType, List<String> types) {
        // 대표 type이 관광지면 그대로. 아니면 신주쿠 골든가이가 뒤에 있는 ramen_restaurant 때문에 맛집이 됨
        if ("tourist_attraction".equals(primaryType)) return SIGHTSEEING;
        List<String> all = new ArrayList<>();
        if (primaryType != null) all.add(primaryType);
        if (types != null) all.addAll(types);

        for (String t : all) {
            PlaceCategory c = byType(t);
            if (c != null) return c;
        }
        // 구체적인 type이 없을 때만 쓰는 넓은 type
        for (String t : all) {
            if (t.equals("tourist_attraction")) return SIGHTSEEING;
            if (t.startsWith("sublocality") || t.equals("neighborhood")) return SIGHTSEEING;  // 도톤보리처럼 동네로 저장한 장소
            if (t.equals("food")) return FOOD;
            if (t.equals("store")) return SHOPPING;
        }
        return OTHER;
    }

    private static PlaceCategory byType(String t) {
        PlaceCategory c = EXACT.get(t);
        if (c != null) return c;
        if (t.endsWith("_restaurant")) return FOOD;
        if (t.endsWith("_store") || t.endsWith("_market")) return SHOPPING;
        if (t.endsWith("_museum")) return CULTURE;
        return null;
    }

    private static void put(PlaceCategory category, String... types) {
        for (String t : types) EXACT.put(t, category);
    }
}
