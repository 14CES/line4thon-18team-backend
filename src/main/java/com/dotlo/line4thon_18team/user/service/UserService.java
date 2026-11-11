package com.dotlo.line4thon_18team.user.service;

import com.dotlo.line4thon_18team.global.error.BusinessException;
import com.dotlo.line4thon_18team.global.error.ErrorCode;
import com.dotlo.line4thon_18team.place.repository.SavedPlaceRepository;
import com.dotlo.line4thon_18team.trip.repository.TripRepository;
import com.dotlo.line4thon_18team.user.dto.MyStatsResponse;
import com.dotlo.line4thon_18team.user.dto.NicknameCheckResponse;
import com.dotlo.line4thon_18team.user.dto.ProfileUpdateRequest;
import com.dotlo.line4thon_18team.user.dto.UserResponse;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // 한글(자음/모음만 있는 것도 허용), 영문, 숫자 2~12자. 공백, 특수문자는 안 됨
    private static final Pattern NICKNAME = Pattern.compile("[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]{2,12}");

    private final UserRepository userRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final TripRepository tripRepository;

    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    // 지금 내 닉네임이면 쓸 수 있다고 본다 (수정 화면에서 그대로 저장할 수 있게)
    public NicknameCheckResponse checkNickname(Long userId, String nickname) {
        validateNickname(nickname);
        return new NicknameCheckResponse(isAvailable(userId, nickname));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        validateNickname(request.nickname());
        if (!isAvailable(userId, request.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_TAKEN);
        }
        User user = findUser(userId);
        String bio = request.bio() == null || request.bio().isBlank() ? null : request.bio().trim();
        user.updateMyProfile(request.nickname(), bio, request.characterId());
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 확인과 저장 사이에 다른 사람이 같은 닉네임을 먼저 저장한 경우 (nickname UNIQUE)
            throw new BusinessException(ErrorCode.NICKNAME_TAKEN);
        }
        return UserResponse.from(user);
    }

    // 찜 기능(BE2)이 생기기 전까지 저장한 코스는 0
    public MyStatsResponse getMyStats(Long userId) {
        findUser(userId);
        return new MyStatsResponse(
                savedPlaceRepository.countInMyTrips(userId),
                0,
                tripRepository.countEndedMyTrips(userId, LocalDate.now()));
    }

    private boolean isAvailable(Long userId, String nickname) {
        return userRepository.findByNickname(nickname)
                .map(owner -> owner.getId().equals(userId))
                .orElse(true);
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || !NICKNAME.matcher(nickname).matches()) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME);
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
