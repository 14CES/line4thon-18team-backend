package com.dotlo.line4thon_18team.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 생성/수정 시간을 자동으로 채워주는 부모 클래스.
 * 이 클래스 자체로는 테이블이 생기지 않고 상속받은 엔티티에 컬럼만 추가됨.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
