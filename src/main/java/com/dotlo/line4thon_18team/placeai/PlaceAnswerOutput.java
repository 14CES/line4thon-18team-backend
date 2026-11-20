package com.dotlo.line4thon_18team.placeai;

/** 장소 AI 답변 출력. */
public record PlaceAnswerOutput(String answer) {

    public PlaceAnswerOutput {
        answer = answer == null ? "" : answer.strip();
    }
}
