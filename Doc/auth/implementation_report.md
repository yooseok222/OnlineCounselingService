# Online Counseling Service - 구현 결과 보고서 (v0.2.0-draft)

## 1. 개요

- 프로젝트: OnlineCounselingService
- 범위: 인증(회원가입/로그인) 기능 초안

## 2. 버전 정보

| 구분 | 버전 | 날짜 | 작성자 |
|------|------|------|--------|
| 초안 | 0.1.0-draft | 2025-05-19 | AI assistant |
| 수정 | 0.2.0-draft | 2025-05-29 | AI assistant |

## 3. 구현 현황

- Spring Security 기본 의존성 및 설정 클래스를 추가하여 세션 기반 인증 골격 구성 (완료).
- 도메인 계층에 `Member`, `Company`, `InviteCode` Aggregate 루트 초안 생성 (완료).
- H2 개발 프로파일 및 테스트용 `application-dev.properties` 추가 (완료).
- Redis 의존성 및 기본 설정 파일 생성 (완료).
- 회원가입/로그인 컨트롤러, 서비스, 매퍼 인터페이스 구현 (완료).
- 초대코드 검증 및 상담원 회원가입 로직 구현 (완료).
- 회원가입 및 로그인 관련 테스트 케이스 구현 (완료):
  - 일반 사용자, 관리자, 상담원 회원가입 성공 및 실패 케이스
  - 로그인 성공 및 실패 케이스 (이메일 미인증, 존재하지 않는 이메일)
  - 초대코드 생성, 검증, 삭제 관련 테스트 케이스

## 4. 기술적 접근 방법

- Domain-Driven Design 패키징: `domain`, `application`, `infrastructure`, `presentation`.
- TDD 체계 수립: JUnit5 + SpringBootTest + H2 인메모리 DB.
- Mockito 설정 개선: `@MockitoSettings(strictness = Strictness.LENIENT)` 적용으로 필요한 모킹만 검증.
- MyBatis 매퍼 XML은 `resources/mapper`로 분리.
- 초대코드 캐싱은 Spring Data Redis `ValueOperations<String, Object>` 활용.
- 회원 유형별(고객, 상담원, 관리자) 차별화된 검증 로직 구현.

## 5. 해결한 문제

- Oracle ↔ H2 타입 호환 이슈를 `oracle.sql` Dialect 대신 `H2Dialect` + schema-h2.sql 로 분리하여 해결.
- Spring Security 6 이상에서 `WebSecurityConfigurerAdapter` 제거 → `SecurityFilterChain` Beans 로 마이그레이션.
- 초대코드 검증 로직 개선으로 상담원 회원가입 시 유효한 회사 정보 연결 보장.
- 리플렉션을 활용한 ID 할당 방식으로 테스트의 안정성 향상.
- Redis 모킹 처리로 초대코드 관련 테스트의 재현성 확보.

## 6. 미해결/추가 과제

- 이메일 인증 SMTP 연동
- 초대코드 만료 로직 최적화
- UI 레이아웃 통합 및 화면 테스트
- 통합 테스트 및 E2E 테스트 작성

## 7. 향후 일정

- v0.3.0: 보안 강화, 코드 리팩토링, 성능 튜닝 
- v0.4.0: 상담원-고객 간 채팅 기능 구현

### 변경 이력

- v0.1.0-draft: 초기 문서 작성
- v0.2.0-draft: 상담원 회원가입 테스트, 관리자/상담원 로그인 테스트, 초대코드 시스템 테스트 내용 추가 