package com.retrip.auth.infra.adapter.in.rest.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retrip.auth.application.config.JwtConfig;
import com.retrip.auth.application.config.UsernamePasswordAuthentication;
import com.retrip.auth.application.in.response.LoginResponse;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class LoginAuthenticationFilter extends OncePerRequestFilter {
    private final JwtConfig jwtConfig;
    private final AuthenticationManager manager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String id = request.getHeader("id");
        String password = request.getHeader("password");
        if (!StringUtils.hasText(id) || !StringUtils.hasText(password)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Authentication authentication = new UsernamePasswordAuthentication(id, password);
        Authentication auth = manager.authenticate(authentication);
        LoginResponse.TokenResponse tokenResponse = generateToken(auth);
        ApiResponse<LoginResponse.TokenResponse> result = ApiResponse.ok(tokenResponse);
        // JSON 직렬화
        String json = getResponseBody(result);

        // 응답 Header 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 응답에 JSON 데이터 작성
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(json);
        response.getWriter().flush();
    }


    public LoginResponse.TokenResponse generateToken(Authentication authentication) {
        String authorities = String.join(",", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        LocalDateTime issuedTime = LocalDateTime.now();
        LocalDateTime accessTokenExpireTime = LocalDateTime.now().plusMinutes(jwtConfig.getAccess().getExpireMin());
        LocalDateTime refreshTokenExpireTime = LocalDateTime.now().plusMinutes(jwtConfig.getRefresh().getExpireMin());

        Date issuedDate = Date.from(issuedTime.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        Date accessTokenExpireDate = Date.from(accessTokenExpireTime.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        Date refreshTokenExpireDate = Date.from(refreshTokenExpireTime.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        String accessToken = Jwts.builder()
                .subject(authentication.getName())
                .claims(
                        Map.of(
                                "username",
                                authentication.getName(),
                                "authorities",
                                authorities
                        )
                )
                .expiration(accessTokenExpireDate)
                .issuedAt(issuedDate)
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(authentication.getName())
                .claims(Map.of("username", authentication.getName(), "authorities", authorities))
                .expiration(refreshTokenExpireDate)
                .issuedAt(issuedDate)
                .signWith(key)
                .compact();
        return new LoginResponse.TokenResponse(accessToken, refreshToken);
    }

    private static String getResponseBody(ApiResponse<LoginResponse.TokenResponse> tokenResponse) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(tokenResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        //로그인만
        return !request.getRequestURI().equals("/login");
    }
}
