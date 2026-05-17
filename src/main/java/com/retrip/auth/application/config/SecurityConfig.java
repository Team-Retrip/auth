package com.retrip.auth.application.config;

import com.retrip.auth.application.in.CustomOAuth2UserService;
import com.retrip.auth.application.in.MemberQueryService;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.infra.adapter.in.rest.filter.JwtAuthenticationFilter;
import com.retrip.auth.infra.adapter.in.rest.filter.LoginAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
        private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final MemberRepository memberRepository;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        HttpSecurity http,
                        UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider,
                        MemberQueryService memberQueryService) throws Exception {

                AuthenticationManagerBuilder authenticationManagerBuilder = http
                                .getSharedObject(AuthenticationManagerBuilder.class);

                authenticationManagerBuilder
                                .authenticationProvider(usernamePasswordAuthenticationProvider)
                                .userDetailsService(memberQueryService);

                return authenticationManagerBuilder.build();
        }

        @Value("${app.cookie.secure:true}")
        private boolean cookieSecure;

        @Value("${app.frontend-callback-url:http://localhost:3000/auth/callback}")
        private String frontendCallbackUrl;

        @Bean
        public LoginAuthenticationFilter loginAuthenticationFilter(
                        JwtConfig jwtConfig,
                        AuthenticationManager authenticationManager,
                        JwtProvider jwtProvider) {
                return new LoginAuthenticationFilter(jwtConfig, authenticationManager, jwtProvider,
                                refreshTokenRepository, memberRepository, cookieSecure);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        LoginAuthenticationFilter loginAuthenticationFilter) throws Exception {

                SecurityContextRepository securityContextRepository = new DelegatingSecurityContextRepository(
                                new RequestAttributeSecurityContextRepository(),
                                new HttpSessionSecurityContextRepository());

                http
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(Customizer.withDefaults())

                                .securityContext(context -> context
                                                .securityContextRepository(securityContextRepository))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(handler -> handler
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                                                        "Unauthorized");
                                                }))

                                .addFilterAt(loginAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authorization -> authorization
                                                                .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                                                // OAuth2LoginAuthenticationFilter(콜백 처리 필터)에도 직접 주입
                                                .withObjectPostProcessor(new org.springframework.security.config.annotation.ObjectPostProcessor<OAuth2LoginAuthenticationFilter>() {
                                                        @Override
                                                        public <O extends OAuth2LoginAuthenticationFilter> O postProcess(O filter) {
                                                                filter.setAuthorizationRequestRepository(cookieAuthorizationRequestRepository);
                                                                return filter;
                                                        }
                                                })
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2LoginSuccessHandler)
                                                .failureHandler((request, response, exception) -> {
                                                        log.error("OAuth2 로그인 실패: {}", exception.getMessage());
                                                        response.sendRedirect(frontendCallbackUrl + "?error=" +
                                                                        java.net.URLEncoder.encode(
                                                                                        exception.getMessage(),
                                                                                        java.nio.charset.StandardCharsets.UTF_8));
                                                }))

                                .authorizeHttpRequests(auth -> auth
                                                // ✅ 수정: /api/users 경로 추가
                                                .requestMatchers(HttpMethod.POST, "/users", "/api/users").permitAll()
                                                .requestMatchers("/login/**", "/oauth2/**", "/auth/reissue",
                                                                "/auth/logout", "/")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST,
                                                                "/auth/find-email",
                                                                "/auth/find-email/send-code",
                                                                "/auth/find-email/confirm",
                                                                "/auth/password-reset/by-verification",
                                                                "/auth/password-reset/by-verification/send-code",
                                                                "/auth/password-reset/by-verification/confirm",
                                                                "/auth/password-reset/by-email",
                                                                "/auth/password-reset",
                                                                "/auth/email/send-code",
                                                                "/auth/email/verify-code")
                                                .permitAll()
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**", "/swagger-resources/**",
                                                                "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/test.html", "/").permitAll()
                                                // ✅ 추가: 본인인증 및 여행 스타일 조회 API 허용
                                                .requestMatchers(HttpMethod.GET, "/api/travel-styles",
                                                                "/api/users/check-nickname")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/auth/verify-identity")
                                                .authenticated()
                                                .anyRequest().authenticated());

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOriginPatterns(List.of(
                                "http://localhost:*",
                                "http://127.0.0.1:*",
                                "https://retrip-web.vercel.app",
                                "https://retrip-web-*.vercel.app",
                                "https://*-*.vercel.app",
                                "https://retrip.io",
                                "https://*.retrip.io",
                                "https://retrip.site",
                                "https://*.retrip.site"));

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                config.setExposedHeaders(List.of("Authorization"));
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
