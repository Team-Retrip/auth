package com.retrip.auth.infra.adapter.in.rest.filter;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.in.request.LoginRequest;
import com.retrip.auth.application.in.request.MemberCreateRequest;
import com.retrip.auth.application.in.request.MemberDeleteRequest;
import com.retrip.auth.application.in.request.MemberUpdateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = true)
class LoginAuthenticationFilterTest extends BaseLoginAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 로그인_성공() throws Exception {
        memberRepository.save(member);

        LoginRequest request = new LoginRequest("test@naver.com", "1234");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void 유저_생성_성공() throws Exception {
        MemberCreateRequest request = new MemberCreateRequest("test@naver.com", "Test1234!", "test", null, null, null, true, false);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.email").isNotEmpty());
    }

    @Test
    void 유저_수정_성공() throws Exception {
        memberRepository.save(member);

        // 로그인해서 토큰 획득
        String accessToken = login("test@naver.com", "1234");

        MemberUpdateRequest request = new MemberUpdateRequest("1234", "Test1234!", "수정 테스트", null, null);

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.email").isNotEmpty());
    }

    @Test
    void 유저_삭제_성공() throws Exception {
        memberRepository.save(member);

        // 로그인해서 토큰 획득
        String accessToken = login("test@naver.com", "1234");

        MemberDeleteRequest request = new MemberDeleteRequest("1234");

        mockMvc.perform(delete("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    private String login(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }
}
