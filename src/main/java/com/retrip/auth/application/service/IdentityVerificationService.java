package com.retrip.auth.application.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.application.dto.response.VerifyIdentityResponse;
import com.retrip.auth.application.out.repository.MemberRepository;
import com.retrip.auth.domain.entity.Member;
import com.retrip.auth.domain.exception.DuplicateUserException;
import com.retrip.auth.domain.exception.MemberNotFoundException;
import com.retrip.auth.domain.exception.PortOneApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final MemberRepository memberRepository;

    @Value("${portone.api_secret}")
    private String apiSecret;

    // [수정] 매개변수 이름을 email -> memberId로 변경하고 UUID로 조회
    @Transactional
    public VerifyIdentityResponse verifyAndSave(String identityVerificationId, String memberId) {
        log.info("🔍 본인인증 검증 시작 - ID: {}, MemberId: {}", identityVerificationId, memberId);

        // 1. 포트원 V2 API로 본인인증 정보 조회
        CertificationInfo certInfo = getCertificationInfo(identityVerificationId);

        // 2. 중복 가입 체크 (CI가 존재할 경우에만)
        if (certInfo.getUniqueKey() != null && memberRepository.existsByCi(certInfo.getUniqueKey())) {
            log.warn("⚠️ 중복 가입 시도 - CI: {}", certInfo.getUniqueKey());
            throw new DuplicateUserException();
        }

        // 3. 사용자 정보 업데이트 (UUID로 조회)
        Member member = memberRepository.findById(UUID.fromString(memberId))
                .orElseThrow(MemberNotFoundException::new);

        // 성별 변환 (MALE -> M, FEMALE -> F)
        String gender = "MALE".equals(certInfo.getGender()) ? "M" : "F";

        log.info("✅ 회원 본인인증 정보 업데이트 - Name: {}, Gender: {}, BirthDate: {}",
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

    private CertificationInfo getCertificationInfo(String identityVerificationId) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.portone.io/identity-verifications/" + identityVerificationId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "PortOne " + apiSecret)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                throw new PortOneApiException("본인인증 정보 조회 실패: " + response.code());
            }

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!json.has("verifiedCustomer")) {
                throw new PortOneApiException("Missing verifiedCustomer in response");
            }

            JsonObject verifiedCustomer = json.getAsJsonObject("verifiedCustomer");

            // 필수 필드
            String name = getStringField(verifiedCustomer, "name");
            String gender = getStringField(verifiedCustomer, "gender");
            String birthday = verifiedCustomer.has("birthDate")
                    ? getStringField(verifiedCustomer, "birthDate")
                    : getStringField(verifiedCustomer, "birthday");

            // [수정] CI, DI는 선택적 필드로 처리 (테스트 환경 대응)
            String ci = getOptionalStringField(verifiedCustomer, "ci");
            String di = getOptionalStringField(verifiedCustomer, "di");

            return CertificationInfo.builder()
                    .name(name)
                    .gender(gender)
                    .birthday(birthday)
                    .uniqueKey(ci)
                    .uniqueInSite(di)
                    .build();
        } catch (IOException e) {
            throw new PortOneApiException("IO Error: " + e.getMessage());
        }
    }

    private String getStringField(JsonObject json, String fieldName) {
        if (!json.has(fieldName) || json.get(fieldName).isJsonNull()) {
            throw new PortOneApiException("Missing required field: " + fieldName);
        }
        return json.get(fieldName).getAsString();
    }

    // [추가] 선택적 필드 추출 메서드
    private String getOptionalStringField(JsonObject json, String fieldName) {
        if (!json.has(fieldName) || json.get(fieldName).isJsonNull()) {
            return null;
        }
        String value = json.get(fieldName).getAsString();
        return value.isEmpty() ? null : value;
    }
}