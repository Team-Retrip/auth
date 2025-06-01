package com.retrip.auth.application.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter
@RequiredArgsConstructor
@ConfigurationProperties("token.jwt")
public class JwtConfig {
    private final String secret;
    private final String header;
    private final String prefix;
    private final AccessConfig access;
    private final RefreshConfig refresh;

    @Getter
    @RequiredArgsConstructor
    public static class AccessConfig {
        private final int expireMin;
    }

    @Getter
    @RequiredArgsConstructor
    public static class RefreshConfig {
        private final int expireMin;
    }
}
