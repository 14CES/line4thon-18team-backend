package com.dotlo.line4thon_18team.course.repository;

import com.dotlo.line4thon_18team.course.entity.CourseBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, Long> {

    // 하트를 연달아 눌러도 1행 (유니크 충돌을 무시). IGNORE 는 외래키 오류도 넘기므로 없는 코스는 부르는 쪽이 먼저 막는다
    @Modifying
    @Query(value = """
            insert ignore into course_bookmarks (user_id, course_id, created_at, updated_at)
            values (:userId, :courseId, now(6), now(6))
            """, nativeQuery = true)
    void insertIgnore(@Param("userId") long userId, @Param("courseId") long courseId);

    @Modifying
    @Query("delete from CourseBookmark b where b.user.id = :userId and b.course.id = :courseId")
    void deleteByUserIdAndCourseId(@Param("userId") long userId, @Param("courseId") long courseId);

    // 최근에 찜한 순
    @Query("select b.course.id from CourseBookmark b where b.user.id = :userId order by b.id desc")
    List<Long> findCourseIdsByUserId(@Param("userId") long userId);

    // 마이 통계 "저장한 코스"
    long countByUserId(Long userId);

    // 회원 탈퇴
    @Modifying
    @Query("delete from CourseBookmark b where b.user.id = :userId")
    void deleteAllByUserId(@Param("userId") long userId);
}
