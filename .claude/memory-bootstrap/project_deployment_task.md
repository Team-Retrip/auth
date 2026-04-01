---
name: 운영 배포 전환 작업 (2026-04-03 주력 과제)
description: 테스트 환경에서 운영 배포 환경으로 전환하는 작업 내용 및 체크리스트
type: project
---

## 목표

테스트 API 키 → 운영 API 키로 교체하고, 소셜 로그인 / PortOne 인증이 실제 환경에서 정상 동작하도록 배포 설정 완전 전환.

**Why:** 현재는 각 서비스 테스트(개발) 키로 동작 중 — 카카오 개발앱, 네이버 테스트 앱, PortOne 테스트 모드 등. 실제 사용자 대상 서비스를 위해 운영 키로 전환 필요.

---

## 환경변수 전환 체크리스트

### JWT (RS256 키 쌍)
- [ ] `JWT_PRIVATE_KEY` — RSA 개인키 (PEM 또는 Base64)
- [ ] `JWT_PUBLIC_KEY` — RSA 공개키

> 키 생성: `openssl genrsa -out private.pem 2048` → `openssl rsa -in private.pem -pubout -out public.pem`
> 환경변수에는 개행 제거 또는 Base64 인코딩 후 저장

### Google OAuth2
- [ ] `GOOGLE_CLIENT_ID` — Google Cloud Console에서 운영용 OAuth 앱 생성
- [ ] `GOOGLE_CLIENT_SECRET`
- 승인된 리다이렉트 URI 추가 필요: `https://api.retrip.io/login/oauth2/code/google`

### 카카오 OAuth2
- [ ] `KAKAO_CLIENT_ID` — 카카오 개발자 콘솔 → 앱 생성 → REST API 키
- [ ] `KAKAO_CLIENT_SECRET` — 카카오 앱 → 보안 → Client Secret
- Redirect URI 등록 필요: `https://api.retrip.io/login/oauth2/code/kakao`
- 동의항목: `profile_nickname`, `account_email` 비즈니스 검수 여부 확인

### 네이버 OAuth2
- [ ] `NAVER_CLIENT_ID` — 네이버 개발자 센터 → 앱 등록
- [ ] `NAVER_CLIENT_SECRET`
- Redirect URI 등록 필요: `https://api.retrip.io/login/oauth2/code/naver`

### PortOne V2 (본인인증)
- [ ] `PORTONE_STORE_ID` — PortOne 콘솔 → 스토어 ID (테스트 → 운영 채널로 전환)
- [ ] `PORTONE_CHANNEL_KEY` — 운영 채널 키 (본인인증 채널)
- [ ] `PORTONE_API_SECRET` — PortOne V2 API Secret
- PortOne 콘솔에서 도메인 화이트리스트 등록: `https://retrip.io`, `https://api.retrip.io`

### AWS S3
- [ ] `AWS_ACCESS_KEY_ID` — 운영용 IAM 사용자 (최소 권한: s3:PutObject, s3:GetObject)
- [ ] `AWS_SECRET_ACCESS_KEY`
- 운영 환경 EC2/ECS에서는 IAM Role 사용 권장 (application-prod.yml은 이미 DefaultCredentialsProvider 설정됨)

### 앱 설정
- [ ] `FRONTEND_CALLBACK_URL` — `https://retrip.io/auth/callback` (운영 도메인으로 변경)

---

## application.yml 현재 설정 확인

`src/main/resources/application.yml` 기준 환경변수 참조 현황:

```yaml
token.jwt.private-key: ${JWT_PRIVATE_KEY}
token.jwt.public-key: ${JWT_PUBLIC_KEY}
spring.security.oauth2.client.registration.google.client-id: ${GOOGLE_CLIENT_ID:your-google-client-id}
spring.security.oauth2.client.registration.google.client-secret: ${GOOGLE_CLIENT_SECRET:your-google-client-secret}
spring.security.oauth2.client.registration.kakao.client-id: ${KAKAO_CLIENT_ID:your-kakao-rest-api-key}
spring.security.oauth2.client.registration.kakao.client-secret: ${KAKAO_CLIENT_SECRET:your-kakao-client-secret}
spring.security.oauth2.client.registration.naver.client-id: ${NAVER_CLIENT_ID:your-naver-client-id}
spring.security.oauth2.client.registration.naver.client-secret: ${NAVER_CLIENT_SECRET:your-naver-client-secret}
portone.public.store-id: ${PORTONE_STORE_ID}
portone.public.channel-key: ${PORTONE_CHANNEL_KEY}
portone.api_secret: ${PORTONE_API_SECRET}
cloud.aws.credentials.access-key: ${AWS_ACCESS_KEY_ID}
cloud.aws.credentials.secret-key: ${AWS_SECRET_ACCESS_KEY}
app.frontend-callback-url: ${FRONTEND_CALLBACK_URL:http://localhost:3000/auth/callback}
```

redirect-uri가 `localhost:8080` 하드코딩되어 있음 — **운영 도메인으로 변경 필요**:
```yaml
# 변경 전
redirect-uri: "http://localhost:8080/login/oauth2/code/google"
# 변경 후 (환경변수화 권장)
redirect-uri: "${APP_BASE_URL:http://localhost:8080}/login/oauth2/code/google"
```

---

## 운영 배포 시 추가 확인 사항

### DB 전환
- 로컬: H2 in-memory → 운영: MySQL
- `application-prod.yml` 에 MySQL datasource 설정 확인
- `ddl-auto: update` → 운영에서는 `validate` 또는 Flyway/Liquibase 전환 권장
- **member_social_provider 테이블 수동 생성 필요** (신규 테이블, 이번 PR에서 추가됨)
- **member 테이블에서 provider_id 컬럼 제거, nickname 컬럼 추가** (마이그레이션 스크립트 필요)

### Docker / CI-CD
- 환경변수를 GitHub Secrets 또는 서버 `.env`에 등록
- `spring.profiles.active=prod` 설정 확인
- application-prod.yml S3 설정: IAM Role 기반이므로 `AWS_ACCESS_KEY_ID` 불필요

### 반드시 삭제할 파일
- `src/main/java/com/retrip/auth/infra/adapter/in/rest/in/DebugController.java`
  → `/debug/tokens` 엔드포인트가 인증 없이 전체 리프레시 토큰 노출, **운영 배포 전 필수 삭제**

---

## 소셜 로그인 운영 전환 플로우 검증 시나리오

1. 카카오 운영 앱으로 로그인 → JWT 정상 발급 확인
2. 네이버 운영 앱으로 로그인 → 동일 이메일 카카오 계정과 자동 연동 확인
3. PortOne 운영 채널로 본인인증 → isVerified=true, name/birthDate 업데이트 확인
4. 소셜 로그인 후 콜백 URL → `https://retrip.io/auth/callback?accessToken=...` 정상 리다이렉트
5. RefreshToken Cookie 운영 도메인 (secure, httpOnly, sameSite=None) 설정 확인
