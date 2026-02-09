package com.retrip.auth.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationInfo {
    private String name;          // 이름
    private String gender;        // 성별 (male/female)
    private String birthday;      // 생년월일 (YYYYMMDD)
    private String uniqueKey;     // CI
    private String uniqueInSite;  // DI
}
