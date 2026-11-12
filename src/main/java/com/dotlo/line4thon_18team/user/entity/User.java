package com.dotlo.line4thon_18team.user.entity;

import com.dotlo.line4thon_18team.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구글 로그인한 사용자
 * 사람 구분은 이메일이 아니라 googleSub로 함.(이메일은 바뀔 수 있어서)
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_google_sub", columnNames = "google_sub"),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구글 계정 고유 번호 (ID 토큰의 sub)
    @Column(name = "google_sub", nullable = false, length = 100)
    private String googleSub;

    @Column(nullable = false)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String profileImageUrl;

    // 지금 유효한 refresh 토큰의 해시 (사용자당 1개, 로그아웃하면 null)
    // 토큰 원문이 아니라 해시만 저장해서 DB가 유출돼도 토큰으로 못 씀
    @Column(length = 64)
    private String refreshTokenHash;

    // 서비스에서 보이는 이름. 가입할 때 정하고, 정하기 전에는 null (사람마다 달라야 함)
    @Column(length = 12)
    private String nickname;

    // 한 줄 소개
    @Column(length = 30)
    private String bio;

    // 기본 캐릭터 번호 (1~10). 이미지는 FE가 번호로 보여준다
    private Integer characterId;

    @Builder
    private User(String googleSub, String email, String name, String profileImageUrl) {
        this.googleSub = googleSub;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    // 다시 로그인할 때 구글 쪽에서 바뀐 정보(이름, 사진 등)를 반영
    public void updateProfile(String email, String name, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    // 마이에서 고친 프로필
    public void updateMyProfile(String nickname, String bio, int characterId) {
        this.nickname = nickname;
        this.bio = bio;
        this.characterId = characterId;
    }

    // 다른 사람에게 보이는 이름. 닉네임을 정하기 전이면 구글 이름
    public String getDisplayName() {
        return nickname != null ? nickname : name;
    }

    // 로그인·재발급할 때 새 refresh 토큰으로 교체 (이전 토큰은 더 이상 못 씀)
    public void updateRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    // 로그아웃
    public void clearRefreshToken() {
        this.refreshTokenHash = null;
    }
}