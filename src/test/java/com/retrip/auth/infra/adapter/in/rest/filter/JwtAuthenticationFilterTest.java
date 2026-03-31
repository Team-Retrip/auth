package com.retrip.auth.infra.adapter.in.rest.filter;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
class JwtAuthenticationFilterTest extends BaseLoginAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void JwtLogin() throws Exception {
        memberRepository.save(member);

        // given
        LoginRequest request = new LoginRequest("test@naver.com", "1234");

        //when
        // headers 생성
        HttpHeaders loginHeader = new HttpHeaders();
        loginHeader.add("id", request.id());
        loginHeader.add("password", request.password());
        MvcResult mvcResult =  mockMvc.perform(get("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(loginHeader))
                .andExpect(status().isOk())
                .andReturn(); //로그인 결과
        // 응답 본문 추출
        String responseBody = mvcResult.getResponse().getContentAsString();

        // JSON 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        JsonNode data = jsonNode.get("data");
        String accessToken = data.get("accessToken").asText();
        String refreshToken = data.get("refreshToken").asText();


        // when & then
        HttpHeaders jwtHeader = new HttpHeaders();
        jwtHeader.add("Authorization", "Bearer " + accessToken);
        mockMvc.perform(get("/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(jwtHeader))
                .andExpect(status().isOk())
                .andExpect(content().string("test@naver.com"));
    }
}
