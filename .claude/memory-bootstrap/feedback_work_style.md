---
name: 협업 방식 및 피드백
description: 이 개발자와 작업할 때의 스타일 가이드 및 주의사항
type: feedback
---

## 응답 스타일

- 답변은 짧고 직접적으로. 불필요한 전제나 재확인 없이 바로 핵심
- 코드 수정 시 파일 전체보다 변경 부분 위주로 설명
- 설계 결정을 내릴 때는 근거(업계 표준, 타 서비스 사례)를 함께 제시
- 한국어로 소통

**Why:** 개발자가 이미 맥락을 알고 있으므로 재설명보다 빠른 실행을 선호.
**How to apply:** 질문에는 바로 답하고, 확인이 필요한 경우에만 질문.

## 코드 작업 시 주의사항

- 팀원 코드(searchMembers, getMembersByIds, ImageService 등)는 건드리지 말 것
- 헥사고날 아키텍처 패키지 구조 준수 (controller는 infra, usecase는 application/in)
- `hasPassword()` 패턴 일관 사용 — `provider == "local"` 비교 방식 사용 금지
- 소셜 유저 관련 로직은 항상 `hasPassword()` 분기로 처리
- DB: 로컬은 H2, 운영은 MySQL — 방언 차이 주의 (JPQL 사용 권장, native query 지양)

## 세션 간 컨텍스트 인계

- 이 파일들(`memory-bootstrap/`)을 읽으면 이전 세션의 전체 컨텍스트를 복원할 수 있음
- 새 기기에서 세션 시작 시: "memory-bootstrap 폴더를 읽고 이 기기 메모리에 저장해줘"
- 읽기 완료 후 bootstrap 파일은 삭제하거나 .gitignore에 추가
