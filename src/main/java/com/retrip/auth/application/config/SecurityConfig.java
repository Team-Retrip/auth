package com.retrip.auth.application.config;

import com.retrip.auth.application.in.MemberQueryService;
import com.retrip.auth.infra.adapter.in.rest.filter.JwtAuthenticationFilter;
import com.retrip.auth.infra.adapter.in.rest.filter.LoginAuthenticationFilter;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //암호 인코더 정의
    }

    @Bean
    public LoginAuthenticationFilter loginAuthenticationFilter(JwtConfig jwtConfig, AuthenticationManager authenticationManager) {
        return new LoginAuthenticationFilter(jwtConfig, authenticationManager);
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
        return http.authenticationProvider(usernamePasswordAuthenticationProvider)
                .userDetailsService(memberQueryService)
                .getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://3.39.117.218:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, LoginAuthenticationFilter loginAuthenticationFilter, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .addFilterAt(loginAuthenticationFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("users").permitAll()
                            .requestMatchers(
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/swagger-resources/**",
                                    "/webjars/**"
                            ).permitAll()
                            .anyRequest().authenticated();
                }
        );

        return http.build();
    }
}
