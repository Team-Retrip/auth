package com.retrip.auth.application.service.recovery;

import com.retrip.auth.domain.entity.Member;

/**
 * 본인인증 기반 회원 조회 결과.
 * wasJustVerified: 이번 요청에서 처음으로 본인인증이 연결된 경우 true (이미 인증된 사용자면 false)
 */
public record VerificationResult(Member member, boolean wasJustVerified) {
}
