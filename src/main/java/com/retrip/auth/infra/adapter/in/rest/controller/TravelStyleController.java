package com.retrip.auth.infra.adapter.in.rest.controller;

import com.retrip.auth.application.service.TravelStyleService;
import com.retrip.auth.domain.entity.TravelStyle;
import com.retrip.auth.infra.adapter.in.rest.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/travel-styles")
@RequiredArgsConstructor
public class TravelStyleController {

    private final TravelStyleService travelStyleService;

    /**
     * 모든 여행 스타일 조회
     */
    @GetMapping
    public ApiResponse<List<TravelStyle>> getAllTravelStyles() {
        List<TravelStyle> travelStyles = travelStyleService.getAllTravelStyles();
        return ApiResponse.ok(travelStyles);
    }
}
