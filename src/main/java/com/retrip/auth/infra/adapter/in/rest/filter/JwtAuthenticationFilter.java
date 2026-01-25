package com.retrip.auth.infra.adapter.in.rest.filter;

import com.retrip.auth.application.config.CustomUserDetails;
import com.retrip.auth.application.config.JwtProvider;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.vo.MemberEmail;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final JwtProvider jwtProvider;
    // 1. 회원 상태 확인을 위한 Repository 주입
    private final MemberRepository memberRepository;

    // auth/src/main/java/com/retrip/auth/infra/adapter/in/rest/filter/JwtAuthenticationFilter.java

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getToken(request.getHeader(AUTHORIZATION_HEADER));

        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            try {
                Authentication auth = jwtProvider.getAuthentication(token);
                if (auth != null) {
                    // [수정] Principal 객체에서 ID를 안전하게 추출
                    Object principal = auth.getPrincipal();
                    String memberIdStr;

                    if (principal instanceof CustomUserDetails) {
                        memberIdStr = ((CustomUserDetails) principal).getUsername();
                    } else if (principal instanceof String) {
                        memberIdStr = (String) principal;
                    } else {
                        throw new RuntimeException("알 수 없는 Principal 타입입니다.");
                    }

                    UUID memberId = UUID.fromString(memberIdStr);

                    // DB에서 회원 상태 확인 (삭제된 회원인지 체크)
                    boolean isDeleted = memberRepository.findById(memberId)
                            .map(Member::getIsDeleted)
                            .orElse(true);

                    if (isDeleted) {
                        log.warn("❌ 탈퇴한 회원의 접근 시도: {}", memberId);
                        sendErrorResponse(response, "Member is deleted or not found");
                        return;
                    }

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("✅ JWT 인증 성공: {}", memberIdStr);
                }
            } catch (Exception e) {
                log.error("❌ JWT 인증 에러: ", e);
                SecurityContextHolder.clearContext();
                // 인증 에러 시 즉시 401 반환하여 무한 리디렉션 방지
                sendErrorResponse(response, "Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // 401 Unauthorized 에러 응답을 보내는 헬퍼 메소드
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"success\":false, \"status\":401, \"message\":\"%s\"}", message));
    }

    private String getToken(String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/users") && "POST".equals(method)) {
            return true;
        }

        return path.startsWith("/login")
                || path.startsWith("/oauth2")
                || path.startsWith("/auth/reissue")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.startsWith("/debug");
    }
}