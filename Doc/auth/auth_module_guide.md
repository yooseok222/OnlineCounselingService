# 인증 모듈 가이드 (Auth Module Guide)

## 1. 개요

본 문서는 온라인 상담 서비스 플랫폼의 인증(Authentication) 모듈 구조와 사용법을 설명합니다. 이 모듈은 회원가입, 로그인, 권한 관리 등 사용자 인증 관련 기능을 담당합니다.

**버전**: v0.2.0-draft (2025-05-19)

## 2. 모듈 아키텍처

인증 모듈은 다음과 같은 계층 구조로 설계되었습니다.

```
kr.or.kosa.onlinecounselingservice
├── domain
│   ├── member
│   │   ├── model
│   │   │   ├── Member.java
│   │   │   └── MemberType.java
│   │   └── service
│   │       ├── MemberService.java (interface)
│   │       └── MemberServiceImpl.java
│   ├── company
│   │   ├── model
│   │   │   └── Company.java
│   │   └── service
│   │       ├── CompanyService.java (interface)
│   │       └── CompanyServiceImpl.java
│   └── invitecode
│       ├── model
│       │   └── InviteCode.java
│       └── service
│           ├── InviteCodeService.java (interface)
│           └── InviteCodeServiceImpl.java
├── application
│   ├── dto
│   │   ├── request
│   │   │   ├── LoginRequest.java
│   │   │   └── RegisterRequest.java
│   │   └── response
│   │       ├── LoginResponse.java
│   │       └── RegisterResponse.java
│   └── service
│       ├── AuthService.java (interface)
│       └── AuthServiceImpl.java
├── infrastructure
│   ├── config
│   │   ├── SecurityConfig.java
│   │   └── RedisConfig.java
│   ├── persistence
│   │   ├── mybatis
│   │   │   ├── mapper
│   │   │   │   ├── MemberMapper.java
│   │   │   │   └── CompanyMapper.java
│   │   │   └── typehandler
│   │   │       └── MemberTypeHandler.java
│   │   └── redis
│   │       └── InviteCodeRepository.java
│   └── security
│       ├── CustomUserDetailsService.java
│       └── SecurityUser.java
└── presentation
    ├── controller
    │   ├── AuthController.java
    │   └── VerificationController.java
    └── advice
        └── AuthControllerAdvice.java
```

## 3. 주요 클래스 및 인터페이스

### 3.1 도메인 계층 (Domain Layer)

#### Member (회원)
- `Member`: 사용자 도메인 모델의 추상 클래스
- `MemberType`: 사용자 유형 열거형 (CUSTOMER, COUNSELOR, ADMIN)
- `Client`: 고객 회원 모델 클래스
- `Agent`: 상담원 회원 모델 클래스
- `Admin`: 관리자 회원 모델 클래스
- `MemberService`: 회원 관련 비즈니스 로직 인터페이스

#### Company (회사)
- `Company`: 회사 도메인 모델
- `CompanyService`: 회사 관련 비즈니스 로직 인터페이스

#### InviteCode (초대코드)
- `InviteCode`: 초대코드 도메인 모델
- `InvitationRequest`: 초대코드 생성 요청 DTO
- `InvitationResponse`: 초대코드 응답 DTO
- `InvitationVerifyResponse`: 초대코드 검증 응답 DTO
- `InvitationService`: 초대코드 관련 비즈니스 로직 인터페이스
  - `createInvitation`: 초대코드 생성
  - `verifyInvitation`: 초대코드 검증
  - `deleteInvitation`: 초대코드 삭제
  - `getInvitationsByAdminId`: 관리자 기준 초대코드 조회
  - `getInvitationsByCompanyId`: 회사 기준 초대코드 조회

### 3.2 애플리케이션 계층 (Application Layer)

- `AuthService`: 인증 관련 비즈니스 로직을, 도메인 서비스와 DTO 간 중계
- `LoginRequest/Response`: 로그인 요청/응답 DTO
- `RegisterRequest/Response`: 회원가입 요청/응답 DTO

### 3.3 인프라 계층 (Infrastructure Layer)

#### 설정 (Config)
- `SecurityConfig`: Spring Security 설정
- `RedisConfig`: Redis 설정 (초대코드, 이메일 인증 토큰 저장)

#### 데이터 액세스 (Data Access)
- `MemberMapper`: 회원 정보 데이터 액세스 (MyBatis)
- `ClientMapper`: 고객 정보 데이터 액세스 (MyBatis)
- `AgentMapper`: 상담원 정보 데이터 액세스 (MyBatis)
- `AdminMapper`: 관리자 정보 데이터 액세스 (MyBatis)
- `CompanyMapper`: 회사 정보 데이터 액세스 (MyBatis)
- `InvitationRepository`: 초대코드 데이터 액세스 (Redis)

#### 보안 (Security)
- `CustomUserDetailsService`: Spring Security 인증에 사용되는 사용자 정보 조회
  - `loadUserByUsername`: 이메일로 사용자 정보 조회
  - `loadClientDetails`: 고객 정보 조회
  - `loadAgentDetails`: 상담원 정보 조회
  - `loadAdminDetails`: 관리자 정보 조회
- `SecurityUser`: Spring Security Authentication에서 사용되는 사용자 정보 래퍼

### 3.4 프레젠테이션 계층 (Presentation Layer)

- `AuthController`: 로그인, 회원가입 관련 REST API 또는 화면 진입점
- `VerificationController`: 이메일 인증 처리 컨트롤러
- `AuthControllerAdvice`: 인증 관련 예외 처리기

## 4. 주요 기능 및 API

### 4.1 회원가입 (Registration)

#### 고객 회원가입
```
POST /api/v1/auth/register/customer
```

#### 상담원 회원가입
```
POST /api/v1/auth/register/counselor
```
**주요 파라미터:**
- `email`: 이메일 (필수)
- `password`: 비밀번호 (필수) 
- `name`: 이름 (필수)
- `invitationCode`: 초대코드 (필수) - 유효한 회사 정보와 연결된 코드여야 함
- `phoneNumber`: 전화번호 (선택)
- `address`: 주소 (선택)

#### 관리자 회원가입
```
POST /api/v1/auth/register/admin
```

### 4.2 로그인 (Login)

```
POST /api/v1/auth/login
```
**주요 파라미터:**
- `email`: 이메일 (필수)
- `password`: 비밀번호 (필수)
- `userType`: 사용자 유형 (선택, 기본값: 자동 감지)

**응답:**
- `userId`: 사용자 ID
- `name`: 사용자 이름
- `email`: 이메일
- `role`: 역할 (USER, AGENT, ADMIN)
- `token`: 인증 토큰
- `companyId`: 회사 ID (상담원, 관리자인 경우)
- `companyName`: 회사명 (상담원, 관리자인 경우)
- `state`: 상담원 상태 (상담원인 경우)

### 4.3 이메일 인증 (Email Verification)

```
GET /verify-email?token={token}
```

### 4.4 초대코드 검증 (Invite Code Validation)

```
GET /api/v1/invitations/verify/{code}
```
**응답:**
- `valid`: 유효성 여부 (boolean)
- `companyId`: 회사 ID
- `companyName`: 회사명
- `adminName`: 초대 관리자명
- `errorMessage`: 오류 메시지 (유효하지 않은 경우)

### 4.5 초대코드 생성 (Invite Code Generation)

```
POST /api/v1/invitations
```
**요청:**
- `companyId`: 회사 ID (필수)
- `adminId`: 관리자 ID (필수)
- `adminName`: 관리자명 (필수)
- `expirationDays`: 만료 기간(일) (선택, 기본값 7일)

**응답:**
- `invitationId`: 초대코드 ID
- `invitationCode`: 초대코드
- `companyId`: 회사 ID
- `companyName`: 회사명
- `adminId`: 관리자 ID
- `adminName`: 관리자명
- `expired`: 만료 여부
- `expiredTime`: 만료 시간
- `createdAt`: 생성 시간

## 5. 데이터 흐름 (Data Flow)

### 5.1 회원가입 흐름

#### 고객 회원가입 흐름
1. 클라이언트: 회원 유형 선택(고객) 및 기본 정보 입력
2. `AuthController`: 입력값 검증 및 `AuthService` 호출
3. `AuthService`: 비즈니스 로직 수행 및 `MemberService` 호출
4. `MemberService`: 회원 정보 생성 및 `MemberMapper`를 통해 저장
5. `EmailService`: 인증 이메일 발송
6. 클라이언트: 이메일 인증 링크 클릭
7. `VerificationController`: 토큰 검증 및 계정 활성화

#### 상담원 회원가입 흐름
1. 클라이언트: 회원 유형 선택(상담원) 및 기본 정보 입력
2. 클라이언트: 초대코드 입력
3. `AuthController`: 입력값 검증 및 `AuthService` 호출
4. `AuthService`: 초대코드 검증을 위해 `InvitationService` 호출
5. `InvitationService`: 초대코드 유효성 검증 및 회사 정보 조회
6. `AuthService`: 검증 성공 시 `MemberService` 호출하여 상담원 정보 생성
7. `MemberService`: 상담원 정보 생성 및 `AgentMapper`를 통해 저장
8. `EmailService`: 인증 이메일 발송
9. 클라이언트: 이메일 인증 링크 클릭
10. `VerificationController`: 토큰 검증 및 계정 활성화

#### 관리자 회원가입 흐름
// ... existing code ...

### 5.2 로그인 흐름

1. 클라이언트: 이메일/비밀번호 제출
2. `AuthController`: `AuthService` 호출
3. Spring Security: `CustomUserDetailsService`를 통해 사용자 조회
   - 사용자 유형에 따라 `loadClientDetails`, `loadAgentDetails`, `loadAdminDetails` 중 적절한 메서드 호출
   - 이메일 인증 여부 확인 (미인증 시 `EmailNotVerifiedException` 발생)
4. Spring Security: 비밀번호 검증
5. Spring Security: 인증 성공 시 세션 생성
6. `AuthController`: 응답 반환 (사용자 정보, 리다이렉션 URL 등)

## 6. 설정 및 환경

### 6.1 개발 환경 설정

개발 환경에서는 `application-dev.properties`를 사용합니다:

```properties
# H2 데이터베이스 설정
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# Redis 설정
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=60000

# 메일 서버 설정
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 6.2 운영 환경 설정

운영 환경에서는 Oracle 데이터베이스를 사용하며, 다음과 같은 설정이 필요합니다:

```properties
# Oracle 데이터베이스 설정
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# Redis 설정
spring.redis.host=${REDIS_HOST}
spring.redis.port=${REDIS_PORT}
spring.redis.password=${REDIS_PASSWORD}
spring.redis.timeout=60000

# 메일 서버 설정
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## 7. 보안 고려사항

- 모든 비밀번호는 `BCryptPasswordEncoder`를 사용하여 해시 처리
- 민감한 요청(회원가입, 로그인 등)에는 CSRF 토큰 필요
- 모든 입력값은 `@Valid`를 사용하여 검증
- 초대코드는 단방향 해시와 UUID를 조합하여 생성, 예측 불가능성 보장
- 초대코드는 만료 기간이 설정되어 있으며, Redis에 저장하여 안전하게 관리
- 이메일 인증 토큰은 임의의 UUID를 사용하여 생성
- 상담원 회원가입 시 초대코드 검증을 통해 무단 가입 방지
- 로그인 시 사용자 유형별(고객, 상담원, 관리자) 차별화된 권한 부여

## 8. 제한사항 및 알려진 이슈

- 현재 버전에서는 비밀번호 재설정 기능이 구현되지 않음
- 이메일 인증 후 자동 로그인 기능 미구현
- 다중 디바이스 로그인 시 세션 관리 최적화 필요
- 소셜 로그인(OAuth2) 미지원

## 9. 향후 개선 계획

- 비밀번호 재설정 기능 구현
- OAuth2 소셜 로그인 통합
- 2단계 인증(2FA) 지원
- 계정 활동 로깅 및 이상 행동 탐지
- Redis를 활용한 분산 세션 관리
- 초대코드 재발급 및 회수 기능 구현
- 상담원 활성화/비활성화 관리 기능 강화

## 10. 참고 자료

- [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/index.html)
- [MyBatis 공식 문서](https://mybatis.org/mybatis-3/ko/index.html)
- [Redis 공식 문서](https://redis.io/documentation)
- [OWASP 인증 권장사항](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

### 변경 이력

- v0.1.0-draft (2025-05-19): 최초 작성
- v0.2.0-draft (2025-05-19): 초대코드 검증 및 상담원 회원가입 흐름 상세화, 로그인 프로세스 보강 