package com.dotlo.line4thon_18team.user.dto;

import com.dotlo.line4thon_18team.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String profileImageUrl,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getCreatedAt()
        );
    }
}
