package com.dotlo.line4thon_18team.course.dto;

import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;

/**
 * 인기 코스 가져오기 결과.
 *
 * @param tripId    새로 만든 여행. 프론트는 이 여행 화면으로 이동한다
 * @param itinerary 저장된 첫 일정 (버전 1, source COURSE_ADD)
 */
public record CourseImportResponse(long tripId, ItineraryVersionView itinerary) {
}
