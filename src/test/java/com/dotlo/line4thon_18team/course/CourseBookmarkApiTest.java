package com.dotlo.line4thon_18team.course;

import com.dotlo.line4thon_18team.course.entity.Course;
import com.dotlo.line4thon_18team.course.repository.CourseBookmarkRepository;
import com.dotlo.line4thon_18team.course.repository.CourseRepository;
import com.dotlo.line4thon_18team.place.PlaceCategory;
import com.dotlo.line4thon_18team.user.entity.User;
import com.dotlo.line4thon_18team.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "auth.dev-header-enabled=true")
class CourseBookmarkApiTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired CourseBookmarkRepository bookmarks;

    private User me;
    private Course kyoto;
    private Course osaka;

    @BeforeEach
    void setUp() {
        me = newUser();
        kyoto = Course.create("교토 산책", "일본", "교토", 2);
        kyoto.addDay(1).addStop("test-kyoto", "기요미즈데라", 34.99, 135.78, PlaceCategory.CULTURE, null);
        kyoto = courses.saveAndFlush(kyoto);
        osaka = courses.saveAndFlush(Course.create("오사카 먹방", "일본", "오사카", 1));
    }

    @Test
    void 찜하면_목록에_최근에_찜한_순으로_나오고_두_번_눌러도_한_번이다() throws Exception {
        bookmark(me, kyoto.getId()).andExpect(status().isNoContent());
        bookmark(me, osaka.getId()).andExpect(status().isNoContent());
        bookmark(me, kyoto.getId()).andExpect(status().isNoContent());   // 이미 찜한 코스

        assertThat(bookmarks.count()).isEqualTo(2);
        list(me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(osaka.getId()))
                .andExpect(jsonPath("$[1].id").value(kyoto.getId()))
                .andExpect(jsonPath("$[1].title").value("교토 산책"))
                .andExpect(jsonPath("$[1].stopCount").value(1));
        list(newUser()).andExpect(jsonPath("$.length()").value(0));   // 남의 찜은 안 보인다
    }

    @Test
    void 찜_해제는_찜하지_않았어도_204() throws Exception {
        bookmark(me, kyoto.getId());

        unbookmark(me, kyoto.getId()).andExpect(status().isNoContent());
        unbookmark(me, kyoto.getId()).andExpect(status().isNoContent());

        list(me).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 없는_코스는_404_비로그인은_401() throws Exception {
        bookmark(me, -1L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
        mvc.perform(post("/api/courses/{id}/bookmark", kyoto.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/courses/bookmarks")).andExpect(status().isUnauthorized());
        assertThat(bookmarks.count()).isZero();
    }

    private ResultActions bookmark(User user, long courseId) throws Exception {
        return mvc.perform(post("/api/courses/{id}/bookmark", courseId).header("X-User-Id", user.getId()));
    }

    private ResultActions unbookmark(User user, long courseId) throws Exception {
        return mvc.perform(delete("/api/courses/{id}/bookmark", courseId).header("X-User-Id", user.getId()));
    }

    private ResultActions list(User user) throws Exception {
        return mvc.perform(get("/api/courses/bookmarks").header("X-User-Id", user.getId()));
    }

    private User newUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return users.save(User.builder().googleSub("sub-bookmark-" + id).email(id + "@test.com").name("테스트").build());
    }
}
