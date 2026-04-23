package com.retrip.auth.application.service.recovery;

import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.infra.adapter.out.external.PortOneApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortOneVerificationStrategy {

    private final MemberRepository memberRepository;
    private final PortOneApiClient portOneApiClient;

    public CertificationInfo getCertificationInfo(String impUid) {
        return portOneApiClient.getCertificationInfo(impUid);
    }

    public Optional<Member> findByCi(String ci) {
        if (ci == null) return Optional.empty();
        return memberRepository.findByCiAndIsDeletedFalse(ci);
    }

    public List<Member> findCandidates(String name, String birthDate) {
        return memberRepository.findByNameAndBirthDateAndIsDeletedFalse(name, birthDate);
    }

    @Transactional
    public void linkCi(Member member, CertificationInfo certInfo) {
        String gender = "MALE".equals(certInfo.getGender()) ? "M" : "F";
        member.updateIdentityVerification(
                certInfo.getName(), gender, certInfo.getBirthday(),
                certInfo.getUniqueKey(), certInfo.getUniqueInSite());
    }
}
