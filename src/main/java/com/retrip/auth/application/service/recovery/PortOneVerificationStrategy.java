package com.retrip.auth.application.service.recovery;

import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.infra.adapter.out.external.PortOneApiClient;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortOneVerificationStrategy {

    private final MemberRepository memberRepository;
    private final PortOneApiClient portOneApiClient;

    /**
     * PortOne 본인인증 결과로 회원을 조회한다. HTTP 호출은 호출자가 선행해야 한다.
     *
     * 1. CI로 직접 조회 (이미 본인인증된 사용자) → wasJustVerified=false
     * 2. CI 미매칭 시 이름+생년월일로 조회 (미인증 사용자) → CI 자동 연결, wasJustVerified=true
     */
    @Transactional
    public VerificationResult findMemberByCert(CertificationInfo certInfo) {
        String ci = certInfo.getUniqueKey();

        // 1. CI로 먼저 조회 — 이미 인증된 사용자
        if (ci != null) {
            Optional<Member> byCI = memberRepository.findByCiAndIsDeletedFalse(ci);
            if (byCI.isPresent()) {
                return new VerificationResult(byCI.get(), false);
            }
        }

        // 2. 이름 + 생년월일로 조회 — 미인증 사용자
        String name = certInfo.getName();
        String birthDate = certInfo.getBirthday();
        List<Member> candidates = memberRepository.findByNameAndBirthDateAndIsDeletedFalse(name, birthDate);

        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_BY_VERIFICATION);
        }
        if (candidates.size() > 1) {
            log.warn("이름+생년월일 중복 계정 존재 - name: {}, birthDate: {}", name, birthDate);
        }

        Member member = candidates.get(0);

        // 3. 본인인증 처리 — 이번 요청에서 최초 연결
        if (ci != null) {
            String gender = "MALE".equals(certInfo.getGender()) ? "M" : "F";
            member.updateIdentityVerification(certInfo.getName(), gender, birthDate, ci, certInfo.getUniqueInSite());
            log.info("미인증 사용자 본인인증 처리 완료 - memberId: {}", member.getId());
        }

        return new VerificationResult(member, true);
    }

    /**
     * impUid로 PortOne API를 호출해 본인인증 정보를 조회한다.
     * DB 트랜잭션 밖에서 호출해야 커넥션 점유 시간을 줄일 수 있다.
     */
    public CertificationInfo getCertificationInfo(String impUid) {
        return portOneApiClient.getCertificationInfo(impUid);
    }
}
