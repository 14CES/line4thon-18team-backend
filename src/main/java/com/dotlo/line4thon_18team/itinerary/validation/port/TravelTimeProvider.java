package com.dotlo.line4thon_18team.itinerary.validation.port;

/**
 * 이동시간 함수 (Routes API). 서버에서는 PlaceTravelTimeProvider가 BE2 place.TravelTime을 연결한다.
 *
 * <p>수단을 직접 고르는 함수는 {@link TravelLookup#mode()}에 실제 수단을, 추정치는 {@link TravelLookup#estimated()}로 알려준다.
 *
 * <p>합의할 점:
 * <ul>
 *   <li>경로가 없을 때는 {@link TravelLookup.Status#NO_ROUTE}, API 오류·한도 초과는 {@link TravelLookup.Status#ERROR}로 구분
 *       (기능 상세: 경로 없음 → "이동 경로를 찾지 못했어요", API 오류 → 직선거리 추정치 + '추정' 표시)</li>
 *   <li>예외를 던져도 검증기가 ERROR로 처리하지만, 가능하면 상태로 돌려주기</li>
 *   <li>대중교통(TRANSIT)이 일본에서 실제로 결과가 나오는지 먼저 확인</li>
 * </ul>
 * 한 번에 여러 구간을 묻는 Route Matrix로 구현해도 되고, 검증기는 구간마다 한 번씩 호출한다.
 */
public interface TravelTimeProvider {

    TravelLookup lookup(TravelQuery query);
}
