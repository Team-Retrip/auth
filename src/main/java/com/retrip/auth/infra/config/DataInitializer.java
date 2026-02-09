package com.retrip.auth.infra.config;

import com.retrip.auth.application.service.TravelStyleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TravelStyleService travelStyleService;

    @Override
    public void run(String... args) {
        log.info("여행 스타일 데이터 초기화 시작...");
        travelStyleService.initializeTravelStyles();
        log.info("여행 스타일 데이터 초기화 완료!");
    }
}
