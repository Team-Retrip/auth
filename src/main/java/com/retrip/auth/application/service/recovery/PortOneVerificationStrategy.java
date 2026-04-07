package com.retrip.auth.application.service.recovery;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.PortOneApiException;
import com.retrip.auth.domain.exception.common.BusinessException;
import com.retrip.auth.domain.exception.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortOneVerificationStrategy {

    private final MemberRepository memberRepository;

    @Value("${portone.api_secret}")
    private String apiSecret;

    /**
     * impUid로 본인인증 후 매칭되는 Member를 반환한다.
     * <p>
     * 1. CI로 직접 조회 (본인인증 이력 있는 사용자)
     * 2. CI 미매칭 시 이름+생년월일로 조회 (미인증 사용자) → 자동으로 CI 연결
     */
    @Transactional
    public Member findMember(String impUid) {
        CertificationInfo certInfo = getCertificationInfo(impUid);
        String ci = certInfo.getUniqueKey();

        // 1. CI로 먼저 조회
        if (ci != null) {
            Optional<Member> byCI = memberRepository.findByCiAndIsDeletedFalse(ci);
            if (byCI.isPresent()) {
                return byCI.get();
            }
        }

        // 2. 이름 + 생년월일로 조회 (미인증 사용자)
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

        // 3. 본인인증 처리 — PortOne 데이터를 source of truth로 덮어씀 (기존 updateIdentityVerification과 동일 정책)
        if (ci != null) {
            String gender = "MALE".equals(certInfo.getGender()) ? "M" : "F";
            member.updateIdentityVerification(certInfo.getName(), gender, birthDate, ci, certInfo.getUniqueInSite());
            log.info("미인증 사용자 본인인증 처리 완료 - memberId: {}", member.getId());
        }

        return member;
    }

    private CertificationInfo getCertificationInfo(String impUid) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.portone.io/identity-verifications/" + impUid;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "PortOne " + apiSecret)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new PortOneApiException("본인인증 조회 실패: " + response.code());
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("verifiedCustomer")) {
                throw new PortOneApiException("Missing verifiedCustomer in response");
            }

            JsonObject vc = json.getAsJsonObject("verifiedCustomer");
            String birthday = vc.has("birthDate")
                    ? getStringField(vc, "birthDate")
                    : getStringField(vc, "birthday");

            return CertificationInfo.builder()
                    .name(getStringField(vc, "name"))
                    .gender(getStringField(vc, "gender"))
                    .birthday(birthday)
                    .uniqueKey(getOptionalStringField(vc, "ci"))
                    .uniqueInSite(getOptionalStringField(vc, "di"))
                    .build();
        } catch (IOException e) {
            throw new PortOneApiException("IO Error: " + e.getMessage());
        }
    }

    private String getStringField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new PortOneApiException("Missing required field: " + field);
        }
        return json.get(field).getAsString();
    }

    private String getOptionalStringField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) return null;
        String v = json.get(field).getAsString();
        return v.isEmpty() ? null : v;
    }
}
