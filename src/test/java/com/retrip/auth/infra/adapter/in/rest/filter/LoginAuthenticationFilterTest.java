package com.retrip.auth.infra.adapter.in.rest.filter;


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

    @Test
     void 로그인_성공() throws Exception {
        memberRepository.save(member);

        // given
        LoginRequest request = new LoginRequest("test@naver.com", "1234");

        //when
        // headers 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("id", request.id());
        headers.add("password", request.password());

        // when & then
        mockMvc.perform(get("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void 유저_생성_성공() throws Exception {
        // given
        MemberCreateRequest request = new MemberCreateRequest("test@naver.com", "1234", "test", null, null, true, false);

        //when
        String json = new ObjectMapper().writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.email").isNotEmpty());
    }
    @Test
    void 유저_수정_성공() throws Exception {
        memberRepository.save(member);

        // given
        MemberUpdateRequest request = new MemberUpdateRequest("1234", "1111", "수정 테스트", null, null);
        //when
        String json = new ObjectMapper().writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.email").isNotEmpty());
    }

    @Test
    void 유저_삭제_성공() throws Exception {
        memberRepository.save(member);

        // given
        MemberDeleteRequest request = new MemberDeleteRequest("1234");
        //when
        String json = new ObjectMapper().writeValueAsString(request);

        // when & then
        mockMvc.perform(delete("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
