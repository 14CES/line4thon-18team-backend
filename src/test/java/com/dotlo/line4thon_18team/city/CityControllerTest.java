package com.dotlo.line4thon_18team.city;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dotlo.line4thon_18team.auth.jwt.JwtProvider;
import com.dotlo.line4thon_18team.global.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CityController.class)
@Import({SecurityConfig.class, JwtProvider.class})
class CityControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void 도시_목록() throws Exception {
        mvc.perform(get("/api/cities"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("오사카"))
                .andExpect(jsonPath("$[0].timezone").value("Asia/Tokyo"));
    }
}
