package com.retrip.auth.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 30, message = "소개는 최대 30자까지 입력 가능합니다.")
    private String bio;

    @Size(max = 4, message = "MBTI는 4자리여야 합니다.")
    private String mbti;

    private String profileImageUrl;

    @Size(max = 3, message = "여행 스타일은 최대 3개까지 선택 가능합니다.")
    private List<String> travelStyles;
}
