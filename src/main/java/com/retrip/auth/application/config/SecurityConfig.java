package com.retrip.auth.application.config;

import com.retrip.auth.application.in.CustomOAuth2UserService;
import com.retrip.auth.application.in.MemberQueryService;
import com.retrip.auth.application.out.repository.RefreshTokenRepository;
import com.retrip.auth.infra.adapter.in.rest.filter.JwtAuthenticationFilter;
import com.retrip.auth.infra.adapter.in.rest.filter.LoginAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    // [필수] 필터 생성을 위해 필요한 의존성 주입
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LoginAuthenticationFilter loginAuthenticationFilter(JwtConfig jwtConfig, AuthenticationManager authenticationManager) {
        // [필수] 생성자 인자 4개 확인
        return new LoginAuthenticationFilter(jwtConfig, jwtProvider, authenticationManager, refreshTokenRepository);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtConfig jwtConfig) {
        return new JwtAuthenticationFilter(jwtConfig);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider,
            MemberQueryService memberQueryService) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(usernamePasswordAuthenticationProvider)
                .userDetailsService(memberQueryService)
                .and()
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "https://www.retrip.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, LoginAuthenticationFilter loginAuthenticationFilter, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // [🔥핵심 수정] 이 두 줄이 없어서 에러가 난 겁니다! 꼭 추가하세요.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // JWT 사용 시 세션 Stateless 설정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 필터 위치 설정
                // ✔ 여기서 UsernamePasswordAuthenticationFilter보다 "앞"에 배치해야 함
                .addFilterBefore(loginAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ✔ JWT는 LoginAuthenticationFilter 뒤에서 실행 가능
                .addFilterAfter(jwtAuthenticationFilter, LoginAuthenticationFilter.class)

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        http.authorizeHttpRequests(auth -> {
            auth
                    .requestMatchers("/users", "/login/**", "/oauth2/**", "/").permitAll()
                    .requestMatchers("/auth/reissue", "/auth/logout").permitAll()
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**"
                    ).permitAll()
                    .anyRequest().authenticated();
        });

        return http.build();
    }
}