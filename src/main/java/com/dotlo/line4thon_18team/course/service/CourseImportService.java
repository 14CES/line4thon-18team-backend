package com.dotlo.line4thon_18team.course.service;

import com.dotlo.line4thon_18team.city.CityController;
import com.dotlo.line4thon_18team.course.dto.CourseImportRequest;
import com.dotlo.line4thon_18team.course.dto.CourseImportResponse;
import com.dotlo.line4thon_18team.course.entity.Course;
import com.dotlo.line4thon_18team.course.entity.CourseDay;
import com.dotlo.line4thon_18team.course.entity.CourseStop;
import com.dotlo.line4thon_18team.course.repository.CourseDayRepository;
import com.dotlo.line4thon_18team.course.repository.CourseRepository;
import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.itinerary.ItineraryItemId;
import com.dotlo.line4thon_18team.itinerary.ItineraryValidationService;
import com.dotlo.line4thon_18team.itinerary.validation.model.DayDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ItineraryDraft;
import com.dotlo.line4thon_18team.itinerary.validation.model.ValidationOrigin;
import com.dotlo.line4thon_18team.itinerary.validation.model.VisitDraft;
import com.dotlo.line4thon_18team.itinerary.validation.result.ValidationResult;
import com.dotlo.line4thon_18team.itinerary.version.dto.ItineraryVersionView;
import com.dotlo.line4thon_18team.itinerary.version.entity.ItinerarySource;
import com.dotlo.line4thon_18team.itinerary.version.service.ItineraryService;
import com.dotlo.line4thon_18team.place.entity.SavedPlace;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.entity.Companion;
import com.dotlo.line4thon_18team.trip.entity.Trip;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.trip.service.TripDeleteService;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 인기 코스를 내 일정으로 가져온다 (POP-ADD-001). 코스 기간만큼의 새 여행을 만들고 코스 N일차를 여행 N일차에 그대로 넣는다.
 *
 * <p>피그마는 날짜 없이 여행을 만들고 나중에 등록하지만, 여행과 일정이 날짜 기준이라 시작일을 먼저 받는다.
 * 코스 장소는 저장 장소(POPULAR)로 담는다. 코스가 구글 장소 id·좌표·카테고리를 이미 갖고 있어서 구글을 부르지 않는다.
 * 검증이 영업시간·이동시간을 외부에서 조회해서, 기다리는 동안 DB 연결을 잡지 않도록 만들기와 저장만 트랜잭션으로 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CourseImportService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final CourseRepository courseRepository;
    private final CourseDayRepository dayRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final ItineraryValidationService validationService;
    private final ItineraryService itineraryService;
    private final TripDeleteService tripDeleteService;
    private final TransactionTemplate transactionTemplate;

    /**
     * @throws BusinessException 없는 코스(404), 지원하지 않는 도시의 코스(400)
     */
    public CourseImportResponse importAsNewTrip(long userId, long courseId, CourseImportRequest request) {
        NewTrip created = transactionTemplate.execute(status -> createTrip(userId, courseId, request));
        try {
            ValidationResult validation = validationService.validate(created.tripId(), userId, created.draft(),
                    ValidationOrigin.USER_EDITED);
            ItineraryVersionView itinerary = itineraryService.save(created.tripId(), userId,
                    ItinerarySource.COURSE_ADD, null, created.draft(), validation);
            return new CourseImportResponse(created.tripId(), itinerary);
        } catch (RuntimeException e) {
            // 일정 없이 장소만 담긴 여행이 목록에 남지 않게 지운다
            tripDeleteService.deleteTrip(userId, created.tripId());
            throw e;
        }
    }

    private record NewTrip(long tripId, ItineraryDraft draft) {
    }

    /** 코스 기간만큼 여행을 만들고 코스 장소를 저장 장소에 담은 뒤, 여행 날짜에 맞춘 일정을 돌려준다. */
    private NewTrip createTrip(long userId, long courseId, CourseImportRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (CityController.findByName(course.getCity()).isEmpty()) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_CITY);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // 동행은 피그마처럼 나중에 정한다. 안 보내면 혼자
        Companion companion = request.companion() != null ? request.companion() : Companion.ALONE;
        Trip trip = tripRepository.save(Trip.create(user, course.getCity(), request.startDate(),
                request.startDate().plusDays(course.getDayCount() - 1L), companion, request.companionCount()));

        Set<String> saved = new HashSet<>();
        List<DayDraft> days = new ArrayList<>();
        for (CourseDay day : dayRepository.findAllWithStopsByCourseId(courseId)) {
            List<VisitDraft> visits = new ArrayList<>();
            for (CourseStop stop : day.getStops()) {
                // 같은 장소를 여러 날 방문해도 저장 장소는 1행, 일정 항목은 따로다
                if (saved.add(stop.getPlaceId())) {
                    savedPlaceRepository.save(SavedPlace.fromCourse(trip, stop.getPlaceId(), stop.getName(),
                            stop.getLatitude(), stop.getLongitude(), stop.getCategory(), course.getTitle()));
                }
                visits.add(VisitDraft.of(ItineraryItemId.newId(), stop.getPlaceId()));
            }
            days.add(new DayDraft(trip.getStartDate().plusDays(day.getDayNumber() - 1L), visits));
        }
        return new NewTrip(trip.getId(), new ItineraryDraft(days));
    }
}
