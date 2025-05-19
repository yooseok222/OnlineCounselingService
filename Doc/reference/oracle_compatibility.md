# Oracle 11g 호환성 진단 보고서

본 문서는 `OnlineCounselingService` 프로젝트가 **개발 환경(H2)**에서 **운영 환경(Oracle 11g)**으로 전환될 때 발생할 수 있는 호환성 이슈를 진단‧정리한 것입니다. 수정‧보완이 필요한 항목과 대응 방안을 포함합니다.

---

## 1. 데이터베이스 설정 현황

| 프로파일 | 설정 파일 | JDBC URL | Driver | 비고 |
|-----------|-----------|----------|--------|------|
| dev (기본) | `application-dev.properties` | `jdbc:h2:mem:testdb` | `org.h2.Driver` | In-Memory, H2 Dialect |
| default / prod | `application.properties` | `jdbc:oracle:thin:@localhost:1521:XE` | `oracle.jdbc.OracleDriver` | Oracle 11g 예상 |

*프로파일 전환 자체는 `spring.profiles.active` 속성으로 제어되고 있으며, 운영 환경 사용을 위한 최소 설정은 갖추어져 있습니다.*

---

## 2. 스키마(DDL) 분석

* `프로젝트 문서/DDL.SQL` 은 Oracle 11g 문법(`NUMBER`, `VARCHAR2`, `CLOB` 등)에 맞춰 작성되어 있어 **데이터 타입 호환성**에는 큰 문제가 없습니다.
* 그러나 **PK 자동 생성 전략**과 **BOOLEAN 타입** 사용 등 일부 항목이 미비합니다.

### 2.1 PK(Primary Key) 자동 생성

| 테이블 | PK 컬럼 | 현재 매퍼 INSERT 문 | 문제점 |
|--------|---------|---------------------|---------|
| `admin`, `client`, `agent`, `company` 등 대부분 | `*_id` (NUMBER) | `INSERT` 시 PK 컬럼이 빠져 있음 | Oracle 11g에는 `IDENTITY` 지원이 없으므로 자동 증가가 불가. 시퀀스 + 트리거 도입 필요 |

**대응 방안**
1. 테이블별 시퀀스 생성 예시:  
   ```sql
   CREATE SEQUENCE SEQ_ADMIN_ID START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
   ```
2. 트리거 또는 MyBatis `selectKey` 활용하여 시퀀스 값을 `INSERT` 전에 주입.
   ```xml
   <insert id="save" parameterType="Admin">
       <selectKey keyProperty="adminId" resultType="long" order="BEFORE">
           SELECT SEQ_ADMIN_ID.NEXTVAL FROM DUAL
       </selectKey>
       INSERT INTO admins (admin_id, company_id, name, ...)
       VALUES (#{adminId}, #{companyId}, ...)
   </insert>
   ```

### 2.2 BOOLEAN 타입 사용

| 컬럼명 | 매퍼 반환 타입 | 문제 |
|--------|----------------|------|
| `email_verified` (admins) 등 | `BOOLEAN` | Oracle 11g은 `BOOLEAN` 컬럼 타입 미지원 |

**대응 방안**
* `CHAR(1)` 또는 `NUMBER(1)`로 타입 변경 (예: `'Y'/'N'`, `0/1`).
* MyBatis 매퍼에서 `resultType="boolean"` → `resultType="int"` 로 변경 후 Boolean 변환 또는 `typeHandler` 사용.

### 2.3 H2 전용 함수 / 문법

| 위치 | 코드 | 호환성 |
|------|------|--------|
| `AdminMapper.xml` 외 | `COUNT(*) > 0` | Oracle에서 `BOOLEAN`이 아닌 `NUMBER(1)` 반환 → 매핑 수정 필요 |
| `application-dev.properties` | `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect` | 운영 환경에서는 Oracle Dialect 지정 필요 |

---

## 3. 애플리케이션 코드 분석

| 영역 | 현황 | 운영 DB 영향 |
|------|------|--------------|
| MyBatis 매퍼 | CRUD 메서드 구성 완료. 다수의 INSERT 문에서 PK 누락 | 시퀀스 주입 필요 |
| ORM/JPA | 사용하지 않음(MyBatis 전용) | Dialect 설정 무관 |
| Spring Data / JPA Repositories | 없음 | – |
| Validation / Service / Controller | 일부 구현, 일부 빈 클래스 존재 | DB 의존 코드 추가 검토 필요 |
| 테스트 코드 | 미구현 | 시나리오 작성 필요 |

---

## 4. 수정 체크리스트

1. **시퀀스 및 트리거 도입**
   * 테이블별 시퀀스, 트리거 또는 MyBatis `selectKey` 설정.
2. **BOOLEAN → NUMBER(1) / CHAR(1)**
   * 테이블 정의 및 관련 매퍼, 모델 수정.
3. **매퍼 Boolean 반환값 정리**
   * `SELECT COUNT(*) > 0` → `SELECT COUNT(*)` + 자바에서 `>0` 평가.
4. **Hibernate Dialect (추후 JPA 도입 시)**
   * `Oracle12cDialect` 또는 `Oracle10gDialect` 로 지정.
5. **운영 프로필 검증**
   * `spring.profiles.active=prod` 환경에서 `schema.sql`, `data.sql` 자동 실행 비활성화.
6. **통합 테스트**
   * Testcontainers + Oracle XE 사용을 권장하여 CI 환경에서 호환성 자동 검증.

---

## 5. 마이그레이션 절차 제안

1. **DB 사전 준비**
   * Oracle 11g 인스턴스 생성 → `DDL.SQL` 실행 → 시퀀스 + 트리거 추가.
2. **애플리케이션 수정** (위 체크리스트 반영).
3. **로컬 검증**
   * `SPRING_PROFILES_ACTIVE=prod` 로컬 실행 → 모든 API 통합 테스트 수행.
4. **CI/CD 파이프라인**
   * Oracle XE 도커 컨테이너 기반 테스트 단계 추가.
5. **운영 배포**
   * 환경 변수(DB_USER, DB_PASSWORD 등) 설정 → 배포.

---

## 6. 결론

현재 `OnlineCounselingService` 프로젝트는 **스키마 정의 측면**에서는 Oracle 호환 구조를 갖추고 있으나, **애플리케이션 레이어**에서 H2에 의존한 부분(자동 증가, BOOLEAN, 매퍼 반환 타입)으로 인해 _즉시_ Oracle 11g 전환은 어려운 상태입니다.

위 체크리스트를 적용하면 최소 공수로 Oracle 11g 에 완전 호환되는 구조로 개선할 수 있습니다.

> 추가적으로, 초기 PRD에 명시된 기능(초대코드 Redis 저장, Spring Security 기반 인증, TDD, DDD 구조 등)이 아직 완전히 구현되지 않았으므로 기능적 완성도 점검도 병행하시길 권장드립니다.
