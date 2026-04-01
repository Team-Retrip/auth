---
name: auth 도메인 구현 현황
description: auth 도메인의 핵심 설계 결정사항, 구현 완료 내용, 주의사항
type: project
---

## 현재 브랜치

`feat/social-login-multi-provider-nickname` (2026-04-02 커밋, develop PR 오픈 중)

---

## 핵심 설계 결정사항 (팀 합의 완료)

| 항목 | 결정 | 근거 |
|------|------|------|
| 소셜 연동 | 이메일 기준 자동 연동 (Supabase 방식) | 1계정 N소셜 지원, Auth0/Firebase 표준 |
| 소셜 저장 | `member_social_provider` 별도 테이블 | `Member.providerId` 단일 필드 방식 폐기 |
| `Member.provider` 의미 | 마지막 로그인 방식 추적 ("local"/"kakao"/...) | 가입 방식이 아님 — 중요! |
| 소셜 비밀번호 | DB null (소셜 서비스가 관리) | 소셜 유저는 SET 가능, CHANGE 불가 |
| 닉네임 | 가입 시 선택 입력, 미입력 시 이름 자동 대체 | JWT claims에 `nickname` 포함 필수 |
| Access Token | 30분 | 업계 표준 (기존 120분에서 변경) |
| Refresh Token | 14일 | 업계 표준 (기존 2년에서 변경) |
| 알림 설정 | 마스터 토글만 구현 | 세부 토글은 차기 배포 |

---

## DB 스키마 변경 (이번 PR)

### 신규: `member_social_provider`
```sql
CREATE TABLE member_social_provider (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   VARBINARY(16) NOT NULL,
    provider    VARCHAR(20)   NOT NULL,   -- 'kakao'|'naver'|'google'
    provider_id VARCHAR(255)  NOT NULL,   -- 소셜 서비스 고유 ID
    email       VARCHAR(50),
    linked_at   DATETIME(6)   NOT NULL,
    UNIQUE KEY uk_member_provider (member_id, provider),
    UNIQUE KEY uk_provider_provider_id (provider, provider_id)
);
```

### `member` 테이블 변경
- `provider_id` 컬럼 **제거** (member_social_provider로 이전)
- `nickname` 컬럼 **추가** VARCHAR(15)

---

## 구현 완료 파일 목록

### 신규 파일
- `domain/entity/MemberSocialProvider.java` — 소셜 연동 엔티티
- `application/out/repository/MemberSocialProviderRepository.java`
- `application/in/request/SetInitialPasswordRequest.java` — 소셜→로컬 비밀번호 설정

### 주요 수정 파일

**`domain/entity/Member.java`**
- `providerId` 필드 제거
- `nickname` 필드 추가
- `hasPassword()` — `getPasswordValue() != null`
- `setPassword()` — PASSWORD_ALREADY_EXISTS 체크 후 설정
- `updateLastLoginProvider(String provider)` — 로그인 시마다 호출
- `updateNickname(String nickname)`
- `createSocialMember()` — providerId 파라미터 제거

**`application/config/JwtProvider.java`**
- `generateTokens()`, `createToken()`, `getAuthentication()` 모두 `nickname` claim 추가

**`application/in/CustomOAuth2UserService.java`**
- `saveOrUpdate()` 3단계 로직:
  1. providerId 기준 재로그인 → `updateLastLoginProvider`
  2. 이메일 기준 계정 연동 → 새 MemberSocialProvider 저장
  3. 신규 가입 → Member + MemberSocialProvider 저장

**`application/config/OAuth2LoginSuccessHandler.java`**
- `@Value("${app.frontend-callback-url}")` 환경변수화 (하드코딩 제거)
- `authService.saveRefreshToken()` 호출 추가 (버그 수정 — 기존엔 DB 저장 안 됨)

**`infra/adapter/in/rest/filter/LoginAuthenticationFilter.java`**
- `successfulAuthentication()` 에서 `member.updateLastLoginProvider("local")` 추가

**`application/in/MemberService.java`**
- `deleteUser()` — `hasPassword()` 분기 (소셜 유저 NPE 버그 수정)
- `updateUser()` — `isVerified` 잠금 + `hasPassword()` 분기
- `changePassword()` — `!hasPassword()` → Member-007
- `setInitialPassword()` — 신규 (소셜→로컬 비밀번호 설정)
- 팀원 추가 메서드 `searchMembers()`, `getMembersByIds()` 유지

**`infra/adapter/in/rest/in/MemberController.java`**
- `POST /users/password` 추가 — setInitialPassword 호출

---

## 에러 코드 현황

| 코드 | HTTP | 메시지 |
|------|------|--------|
| Member-001 | 401 | 멤버 엔티티를 찾을 수 없습니다 |
| Member-002 | 401 | 비밀번호가 다릅니다 |
| Member-003 | 400 | 탈퇴한 이메일은 재가입할 수 없습니다 |
| Member-004 | 409 | 이미 존재하는 이메일입니다 |
| Member-005 | 409 | 이미 가입된 정보입니다 (CI 중복) |
| Member-006 | 400 | 본인인증 완료 후 이름/생년월일 변경 불가 |
| Member-007 | 403 | 소셜 로그인 계정은 비밀번호 변경 불가 |
| Member-008 | 409 | 이미 비밀번호가 설정된 계정 |
| Image-001 | 400 | 지원하지 않는 이미지 확장자 (팀원 추가) |

---

## 미처리 항목 (주의)

- `DebugController.java` — refresh token 전체 노출, **운영 배포 전 반드시 삭제**
- `application local.yml`, `application.yml.backup`, `new.toml`, `new2.toml` — 로컬 임시 파일, 커밋 제외됨

---

## 스펙 문서 위치

- `C:\Users\p_caso\IdeaProjects\wireframe\spec.html` — Auth API + 화면 상태 명세
- `C:\Users\p_caso\IdeaProjects\wireframe\index.html` — 와이어프레임 (모바일 화면)

**스펙과 현재 구현의 차이 (의도적 변경)**
- 토큰 만료: 스펙은 120분/2년 → 구현은 30분/14일 (팀 합의로 변경)
- GET /users/me 응답: `provider` → `lastLoginProvider` 로 필드명 변경 (프론트 수정 필요)
- ProfileResponse: `linkedProviders`, `hasPassword` 추가 (스펙 미반영, 프론트 협의 필요)
- nickname 필드 추가 (스펙 미반영)
