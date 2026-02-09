package com.retrip.auth.application.service;

import com.retrip.auth.application.out.repository.TravelStyleRepository;
import com.retrip.auth.domain.entity.TravelStyle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelStyleService {

    private final TravelStyleRepository travelStyleRepository;

    public List<TravelStyle> getAllTravelStyles() {
        return travelStyleRepository.findAll();
    }

    @Transactional
    public void initializeTravelStyles() {
        // 기본 여행 스타일 초기화 (이미 존재하면 스킵)
        String[] defaultStyles = {
                "계획철저", "즉흥적", "맛집탐방", "휴양지",
                "가성비", "플렉스", "아침형", "올빼미"
        };

        for (int i = 0; i < defaultStyles.length; i++) {
            String styleName = defaultStyles[i];
            if (!travelStyleRepository.existsByName(styleName)) {
                TravelStyle style = TravelStyle.of(styleName, i + 1);
                travelStyleRepository.save(style);
                log.info("여행 스타일 초기화: {}", styleName);
            }
        }
    }
}
