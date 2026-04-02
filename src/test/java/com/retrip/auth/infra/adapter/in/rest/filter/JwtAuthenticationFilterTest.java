package com.retrip.auth.infra.adapter.in.rest.filter;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.in.request.LoginRequest;
import com.retrip.auth.infra.adapter.in.rest.filter.base.BaseLoginAuthenticationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = true)
class JwtAuthenticationFilterTest extends BaseLoginAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void JwtLogin() throws Exception {
        memberRepository.save(member);

        // 로그인으로 JWT 발급
        LoginRequest request = new LoginRequest("test@naver.com", "1234");
        String loginJson = new ObjectMapper().writeValueAsString(request);
        MvcResult mvcResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        // 토큰 추출
        JsonNode data = new ObjectMapper().readTree(mvcResult.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("accessToken").asText();

        // JWT로 보호된 엔드포인트 호출 (GET /users/me)
        mockMvc.perform(get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@naver.com"));
    }
}
