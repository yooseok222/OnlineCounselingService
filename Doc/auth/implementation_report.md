# Online Counseling Service - 구현 결과 보고서 (v0.1.0-draft)

## 1. 개요

- 프로젝트: OnlineCounselingService
- 범위: 인증(회원가입/로그인) 기능 초안

## 2. 버전 정보

| 구분 | 버전 | 날짜 | 작성자 |
|------|------|------|--------|
| 초안 | 0.1.0-draft | 2025-05-18 | AI assistant |

## 3. 구현 현황

- Spring Security 기본 의존성 및 설정 클래스를 추가하여 세션 기반 인증 골격 구성 (완료).
- 도메인 계층에 `Member`, `Company`, `InviteCode` Aggregate 루트 초안 생성 (완료).
- H2 개발 프로파일 및 테스트용 `application-dev.properties` 추가 (완료).
- Redis 의존성 및 기본 설정 파일 생성 (작성 중).
- 회원가입/로그인 컨트롤러, 서비스, 매퍼 인터페이스 Scaffold 생성 (작성 중).

## 4. 기술적 접근 방법

- Domain-Driven Design 패키징: `domain`, `application`, `infrastructure`, `presentation`.
- TDD 체계 수립: JUnit5 + SpringBootTest + H2 인메모리 DB.
- MyBatis 매퍼 XML은 `resources/mapper`로 분리.
- 초대코드 캐싱은 Spring Data Redis `ValueOperations<String, InviteCode>` 활용 예정.

## 5. 해결한 문제

- Oracle ↔ H2 타입 호환 이슈를 `oracle.sql` Dialect 대신 `H2Dialect` + schema-h2.sql 로 분리하여 해결.
- Spring Security 6 이상에서 `WebSecurityConfigurerAdapter` 제거 → `SecurityFilterChain` Beans 로 마이그레이션.

## 6. 미해결/추가 과제

- 이메일 인증 SMTP 연동
- 초대코드 만료 로직 구현
- UI 레이아웃 통합 및 화면 테스트
- 통합 테스트 및 E2E 테스트 작성

## 7. 향후 일정

- v0.2.0: 기능 완성을 목표로 회원가입 플로우 구현 및 테스트 100% 통과
- v0.3.0: 보안 강화, 코드 리팩토링, 성능 튜닝 