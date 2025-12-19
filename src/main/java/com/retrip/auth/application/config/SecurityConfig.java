package com.retrip.auth.application.config;

import com.retrip.auth.application.in.CustomOAuth2UserService;
import com.retrip.auth.application.in.MemberQueryService;
import com.retrip.auth.infra.adapter.in.rest.filter.JwtAuthenticationFilter;
import com.retrip.auth.infra.adapter.in.rest.filter.LoginAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity // [권장] Spring Security 활성화 어노테이션 추가
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    // [중요] 필터를 new로 생성하지 않고 주입받습니다. (JwtProvider 등의 의존성 해결을 위해)
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager는 Spring Security 6.x 버전부터 빈으로 직접 등록해주어야 컨트롤러나 필터에서 쓸 수 있습니다.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider,
            MemberQueryService memberQueryService) throws Exception {

        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder
                .authenticationProvider(usernamePasswordAuthenticationProvider)
                .userDetailsService(memberQueryService);

        return authenticationManagerBuilder.build();
    }

    /**
     * 로컬 로그인 필터는 AuthenticationManager가 필요하므로 @Bean으로 수동 등록하거나,
     * SecurityFilterChain 내부에서 생성할 수 있습니다. 여기서는 명시적으로 Bean 등록합니다.
     */
    @Bean
    public LoginAuthenticationFilter loginAuthenticationFilter(
            JwtConfig jwtConfig,
            AuthenticationManager authenticationManager,
            JwtProvider jwtProvider) {
        LoginAuthenticationFilter filter = new LoginAuthenticationFilter(jwtConfig, authenticationManager,jwtProvider);
        // 로그인 API URL 변경이 필요하면 여기서 설정 (기본값: /login)
        // filter.setFilterProcessesUrl("/auth/login");
        return filter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginAuthenticationFilter loginAuthenticationFilter) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // [중요] 세션 관리: Stateless (JWT 필수 설정)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 필터 배치
                .addFilterAt(loginAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // 기본 로그인 필터 위치 교체
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // JWT 필터를 그 앞에 배치

                // OAuth2 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                // URL 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // [보안 수정] /users 전체 허용은 위험함 (DELETE는 막아야 함)
                        .requestMatchers(HttpMethod.POST, "/users").permitAll() // 회원가입만 허용

                        .requestMatchers("/login/**", "/oauth2/**", "/auth/reissue", "/").permitAll() // 재발급 등 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 프론트엔드 도메인 허용 (개발용: localhost:3000, 배포용 도메인 추가 필요)
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 쿠키(Refresh Token) 전송 허용
        config.setExposedHeaders(List.of("Authorization")); // 헤더 노출 허용 (필요시)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}