package com.dotlo.line4thon_18team.placeai;

import com.dotlo.line4thon_18team.auth.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 장소 상세 "AI에게 물어보기". */
@RestController
@RequiredArgsConstructor
public class PlaceAnswerController {

    private final PlaceAnswerService service;

    public record AskRequest(
            @NotBlank(message = "질문을 입력해 주세요.")
            @Size(max = PlaceAnswerService.MAX_QUESTION_LENGTH, message = "질문은 200자 이하여야 합니다.")
            String question) {
    }

    public record AskResponse(String answer) {
    }

    @PostMapping("/api/places/{placeId}/ask")
    public ResponseEntity<AskResponse> ask(@LoginUser Long userId, @PathVariable String placeId,
                                           @Valid @RequestBody AskRequest request) {
        return ResponseEntity.ok(new AskResponse(service.ask(placeId, request.question())));
    }
}
