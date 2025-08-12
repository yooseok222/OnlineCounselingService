# 🩺 온라인 상담 서비스 (Online Counseling Service)

## 📋 프로젝트 개요

**비상(VISANG) 온라인 상담 서비스**는 기업과 고객을 전문 상담사와 연결해주는 종합적인 웹 기반 상담 플랫폼입니다.

본 프로젝트는 **Spring Boot**와 **MyBatis**를 기반으로 구축된 엔터프라이즈급 상담 관리 시스템으로, 관리자, 상담사, 고객 간의 원활한 소통을 지원하며 계약서 관리부터 실시간 채팅까지 상담 업무의 전 과정을 디지털화합니다.

### 🎯 프로젝트 목표
- 효율적인 상담사-고객 매칭 시스템 구현
- 실시간 상담 서비스 제공
- 기업별 맞춤형 계약 관리 시스템
- 관리자 중심의 통합 대시보드 운영

## ✨ 주요 기능

### 👑 관리자 시스템 (Admin System)
- **통합 대시보드**: 전체 상담 현황 및 통계 조회
- **상담사 관리**: 상담사 등록, 승인, 상태 관리
- **고객 관리**: 고객 정보 및 상담 이력 관리
- **계약 관리**: 기업별 계약서 생성, 수정, 승인 처리
- **계약서 템플릿 관리**: 재사용 가능한 계약서 양식 관리

### 🎯 상담사 시스템 (Agent System)
- **상담사 대시보드**: 개인 상담 현황 및 스케줄 관리
- **상담사 등록**: 신규 상담사 회원가입 및 프로필 관리
- **상담 상태 관리**: 대기/상담중/휴식 등 실시간 상태 변경
- **상담 이력 관리**: 과거 상담 기록 조회 및 관리

### 💬 실시간 상담 시스템 (Chat System)
- **실시간 채팅**: WebSocket 기반 즉시 메시징
- **상담 세션 관리**: 상담 시작/종료 및 세션 상태 추적
- **채팅 이력 저장**: 모든 대화 내용 데이터베이스 저장

### 🏢 기업 관리 시스템 (Company Management)
- **기업 등록**: 상담 서비스 이용 기업 정보 관리
- **초대 시스템**: 기업 담당자 및 직원 초대 기능
- **기업별 계약 현황**: 개별 기업의 계약 상태 추적

### 📄 문서 관리 시스템
- **PDF 생성**: 계약서 및 상담 보고서 PDF 자동 생성
- **파일 업로드**: 첨부파일 관리 및 저장
- **템플릿 시스템**: 다양한 문서 템플릿 관리

### 🔐 보안 및 인증
- **Spring Security**: 역할 기반 접근 제어 (RBAC)
- **Redis 세션 관리**: 분산 세션 저장 및 관리
- **이메일 인증**: 회원가입 시 이메일 검증
- **비밀번호 암호화**: Hash 기반 안전한 비밀번호 저장

## 🛠️ 기술 스택

### 🔧 Backend Framework
- **Java 17+** - 최신 LTS 버전 기반 개발
- **Spring Boot 3.x** - 엔터프라이즈급 애플리케이션 프레임워크
- **Spring Security** - 인증 및 권한 관리
- **Spring Web MVC** - RESTful API 및 웹 컨트롤러

### 🗄️ 데이터 계층
- **MyBatis** - SQL 매퍼 프레임워크
- **Oracle Database** - 운영환경 메인 데이터베이스
- **H2 Database** - 개발 및 테스트용 인메모리 데이터베이스
- **Redis** - 세션 저장소 및 캐시

### 🎨 Frontend & Template
- **Thymeleaf** - 서버사이드 템플릿 엔진
- **HTML5/CSS3** - 현대적 웹 표준
- **JavaScript** - 클라이언트사이드 동적 기능
- **Bootstrap** - 반응형 UI 프레임워크

### 📡 통신 & 보안
- **WebSocket** - 실시간 채팅 통신
- **HTTPS/TLS** - 안전한 데이터 전송
- **JWT** - 토큰 기반 인증
- **Hash Algorithm** - 비밀번호 암호화

### 🔧 개발 도구 & 빌드
- **Gradle** - 의존성 관리 및 빌드 자동화
- **Git** - 버전 관리 시스템
- **GitHub** - 코드 저장소 및 협업
- **IntelliJ IDEA** - 통합 개발 환경

## 📁 프로젝트 구조

```
OnlineCounselingService/
├── 📂 src/main/java/kr/or/kosa/visang/
│   ├── 📂 advice/                          # 전역 예외 처리
│   │   └── ViewExceptionHandler.java       # 글로벌 에러 핸들러
│   │
│   ├── 📂 common/                          # 공통 컴포넌트
│   │   ├── 📂 config/
│   │   │   ├── 📂 security/               # Spring Security 설정
│   │   │   ├── 📂 redis/                  # Redis 캐시 설정
│   │   │   ├── 📂 mail/                   # 이메일 설정
│   │   │   └── 📂 hash/                   # 암호화 유틸리티
│   │   ├── 📂 email/                      # 이메일 서비스
│   │   ├── 📂 file/                       # 파일 관리 서비스
│   │   └── 📂 redis/                      # Redis 서비스
│   │
│   ├── 📂 domain/                          # 도메인별 비즈니스 로직
│   │   ├── 📂 admin/                      # 관리자 도메인
│   │   ├── 📂 agent/                      # 상담사 도메인
│   │   ├── 📂 chat/                       # 채팅 도메인
│   │   ├── 📂 client/                     # 고객 도메인
│   │   ├── 📂 company/                    # 기업 도메인
│   │   ├── 📂 contract/                   # 계약 도메인
│   │   ├── 📂 contractTemplate/           # 계약 템플릿 도메인
│   │   ├── 📂 invitation/                 # 초대 시스템 도메인
│   │   ├── 📂 pdf/                        # PDF 생성 도메인
│   │   └── 📂 user/                       # 사용자 도메인
│   │
│   └── VisangApplication.java              # Spring Boot 메인 클래스
│
├── 📂 src/main/resources/
│   ├── 📂 mapper/                          # MyBatis SQL 매퍼
│   ├── 📂 templates/                       # Thymeleaf 템플릿
│   ├── 📂 static/                          # 정적 리소스
│   └── 📂 message/                         # 국제화 메시지
│
├── 📂 Doc/                                 # 프로젝트 문서
├── build.gradle                           # Gradle 빌드 설정
└── README.md                              # 프로젝트 설명서
```

## 🔧 주요 컴포넌트

### 🏛️ 코어 컴포넌트
- **VisangApplication.java**: Spring Boot 메인 애플리케이션 클래스
- **GlobalControllerAdvice.java**: 전역 예외 처리 및 공통 응답 관리
- **ViewExceptionHandler.java**: 뷰 계층 예외 처리

### 🔐 보안 컴포넌트
- **Spring Security Configuration**: 사용자 인증 및 권한 관리
- **HashUtil**: 비밀번호 해시 암호화
- **RedisConfig**: 세션 저장소 설정

### 💾 데이터 접근 컴포넌트
- **MyBatis 매퍼 시스템**: XML 기반 SQL 매핑
- **Repository 패턴**: 도메인별 전용 매퍼 인터페이스

### 🌐 웹 컴포넌트
- **도메인별 컨트롤러**: Admin, Agent, Chat, Client 등
- **Thymeleaf 템플릿 시스템**: 서버사이드 렌더링

### 🔧 공통 서비스 컴포넌트
- **EmailService**: SMTP 기반 이메일 발송
- **FileStorageService**: 파일 업로드/다운로드 관리
- **RedisService**: 세션 데이터 저장/조회
- **ChatService**: 실시간 메시징 처리
