package com.retrip.auth.infra.adapter.in.rest.in;

import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.in.AuthService;
import com.retrip.auth.application.in.response.LoginResponse;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        }
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("토큰 재발급 API 성공 - 쿠키 설정 확인")
    void reissue_Api_Success() throws Exception {
        // given
        String oldRefreshToken = "old-cookie-token";
        Cookie cookie = new Cookie("refreshToken", oldRefreshToken);

        LoginResponse.TokenResponse newTokens = new LoginResponse.TokenResponse("new-access", "new-refresh");

        given(authService.reissue(anyString())).willReturn(newTokens);

        // when
        ResultActions result = mockMvc.perform(post("/auth/reissue")
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(cookie().value("refreshToken", "new-refresh"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andDo(print());
    }
}