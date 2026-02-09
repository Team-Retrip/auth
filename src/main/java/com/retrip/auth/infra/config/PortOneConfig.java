package com.retrip.auth.infra.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class PortOneConfig {

    @Value("${portone.public.store-id}")
    private String storeId;

    @Value("${portone.public.channel-key}")
    private String channelKey;

    @Value("${portone.api_secret}")
    private String apiSecret;
}