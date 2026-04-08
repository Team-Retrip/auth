package com.retrip.auth.application.service;

import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.dto.response.VerifyIdentityResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.DuplicateUserException;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.infra.adapter.out.external.PortOneApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final MemberRepository memberRepository;
    private final PortOneApiClient portOneApiClient;

    @Transactional
    public VerifyIdentityResponse verifyAndSave(String identityVerificationId, String memberId) {
        log.info("본인인증 검증 시작 - ID: {}, MemberId: {}", identityVerificationId, memberId);

        CertificationInfo certInfo = portOneApiClient.getCertificationInfo(identityVerificationId);

        if (certInfo.getUniqueKey() != null && memberRepository.existsByCi(certInfo.getUniqueKey())) {
            log.warn("중복 가입 시도 - CI: {}", certInfo.getUniqueKey());
            throw new DuplicateUserException();
        }

        Member member = memberRepository.findById(UUID.fromString(memberId))
                .orElseThrow(MemberNotFoundException::new);

        String gender = "MALE".equals(certInfo.getGender()) ? "M" : "F";

        log.info("회원 본인인증 정보 업데이트 - Name: {}, Gender: {}, BirthDate: {}",
                certInfo.getName(), gender, certInfo.getBirthday());

        member.updateIdentityVerification(
                certInfo.getName(),
                gender,
                certInfo.getBirthday(),
                certInfo.getUniqueKey(),
                certInfo.getUniqueInSite()
        );

        return VerifyIdentityResponse.from(member);
    }
}
