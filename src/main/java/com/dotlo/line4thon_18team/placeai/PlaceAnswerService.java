package com.dotlo.line4thon_18team.placeai;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.global.llm.StructuredLlmExecutor;
import com.dotlo.line4thon_18team.place.PlaceDetails;

/** 장소 AI 답변 (BE3). 장소 정보는 구글에서 가져오므로 여행(tripId) 없이 공개 코스 장소도 묻는다. */
public class PlaceAnswerService {

    static final int MAX_QUESTION_LENGTH = 200;

    private final PlaceDetails placeDetails;
    private final StructuredLlmExecutor llm;
    private final PlaceAnswerTask task;

    public PlaceAnswerService(PlaceDetails placeDetails, StructuredLlmExecutor llm, PlaceAnswerTask task) {
        this.placeDetails = placeDetails;
        this.llm = llm;
        this.task = task;
    }

    public String ask(String placeId, String question) {
        String trimmed = question == null ? "" : question.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_QUESTION_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        PlaceDetails.Detail detail = placeDetails.get(placeId);
        PlaceAnswerInput input = new PlaceAnswerInput(detail.name(), detail.category(), detail.rating(),
                detail.reviewCount(), detail.summary(), detail.address(), trimmed);
        return llm.execute(task, input).value().answer();
    }
}
