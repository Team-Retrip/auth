---
name: Retrip 프로젝트 전체 개요
description: Retrip 팀 프로젝트 도메인 구조, 기술 스택, 팀 구성
type: project
---

## 프로젝트 개요

**Retrip** — 여행 메이트 매칭 앱. 함께 여행할 사람을 찾고 여행을 계획하는 서비스.

## 도메인 구성

| 도메인 | 담당 | 설명 |
|--------|------|------|
| **auth** | 이 개발자 | 회원가입, 로그인, 소셜 로그인, 본인인증, 프로필 |
| **trip** | 다른 팀원(민수민님 외) | 여행 생성, 초대, 신청, 매칭 |

- auth와 trip은 **별도 Spring Boot 서비스**로 분리
- trip 도메인은 auth의 JWT를 검증하고 `UserContext.nickName` 등 JWT claims 사용
- 회원 검색/배치조회는 auth가 API 제공 → trip이 소비

## 기술 스택 (auth 기준)

- **언어/프레임워크**: Java, Spring Boot
- **아키텍처**: 헥사고날 (Ports & Adapters)
- **보안**: Spring Security, JWT RS256
- **DB**: H2 (로컬/테스트), MySQL (운영)
- **ORM**: JPA/Hibernate (ddl-auto: update)
- **소셜 로그인**: Kakao, Naver, Google (Spring OAuth2 Client)
- **본인인증**: PortOne V2 (실명인증)
- **이미지 스토리지**: AWS S3 (Presigned URL 방식)
- **API 문서**: Springdoc (Swagger UI)
- **배포**: Docker, GitHub Actions CI/CD

## 패키지 구조 (헥사고날)

```
com.retrip.auth
├── application
│   ├── config/          ← Spring Security, JWT, OAuth2 설정
│   ├── dto/             ← 내부 DTO (request/response)
│   ├── in/              ← Inbound port (UseCase 구현, OAuth2 서비스)
│   │   ├── request/     ← API 요청 DTO
│   │   ├── response/    ← API 응답 DTO
│   │   └── usercase/    ← UseCase 인터페이스
│   ├── out/repository/  ← Outbound port (Repository 인터페이스)
│   └── service/         ← ProfileService 등 부가 서비스
├── domain
│   ├── entity/          ← JPA 엔티티 (Member, MemberSocialProvider 등)
│   └── exception/       ← 도메인 예외, ErrorCode
└── infra
    └── adapter/in/rest/ ← Controller, Filter, Handler
```

## Git 브랜치 전략

- `main` — 운영 배포
- `develop` — 통합 개발 브랜치 (PR 기준)
- `feat/*` — 기능 브랜치

현재 auth 구현 PR: `feat/social-login-multi-provider-nickname` → develop

## 팀원 커밋 현황 (2026-04-02 기준)

팀원이 develop에 병합한 기능:
- `GET /users/members?ids=` — UUID 목록으로 회원 배치 조회 (trip 도메인 소비)
- `GET /users/search?name=` — 이름 검색 (초대할 사람 찾기)
- `POST /images/presigned-url` — S3 Presigned PUT URL 발급 (프로필 이미지 업로드)
- S3Config: local(StaticCredentials) / prod(IAM DefaultCredentials) 분리

**Why:** trip 도메인이 여행 초대 기능에서 auth 회원 정보 조회가 필요해서 추가됨.
**How to apply:** 이 API들은 auth 보안 로직과 독립적. JWT 필터체인은 그대로 사용.
