package com.dotlo.line4thon_18team.preference;

import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.preference.domain.DayStartPreference;
import com.dotlo.line4thon_18team.preference.domain.TravelPace;
import com.dotlo.line4thon_18team.preference.domain.WalkingPreference;

import java.util.List;

/**
 * 취향 저장 요청 (PUT /api/trips/{tripId}/preferences 본문). 값 검사는 서비스에서 한다.
 *
 * @param rawText 사용자가 처음 입력한 문장 (선택, 최대 500자). 동행자 기능에서 다시 해석할 때 쓸 수 있다.
 */
public record PreferenceInput(TravelPace pace,
                              WalkingPreference walking,
                              DayStartPreference dayStart,
                              List<PlaceCategory> interests,
                              List<PlaceCategory> avoid,
                              List<String> notes,
                              String rawText) {
}
