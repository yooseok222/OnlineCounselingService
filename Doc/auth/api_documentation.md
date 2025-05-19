# Online Counseling Service - API 문서 (v0.1.0-draft)

## 공통 사항

- Base URL: `/api/v1`
- Content-Type: `application/json; charset=UTF-8`
- 인증 방식: 세션 기반 (로그인 성공 시 `JSESSIONID` 쿠키 발급)

---

### 1. 로그인

```
POST /api/v1/auth/login
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | Y | 로그인 이메일 |
| password | string | Y | 비밀번호 |

응답 200
```json
{
  "userId": 1,
  "role": "CUSTOMER", // CUSTOMER | COUNSELOR | ADMIN
  "redirectUrl": "/"
}
```

응답 401
```json
{
  "message": "Invalid credentials"
}
```

---

### 2. 회원가입 ‑ 고객

```
POST /api/v1/auth/register/customer
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | Y | 이메일 |
| password | string | Y | 비밀번호 |
| name | string | Y | 이름 |
| phone | string | Y | 휴대전화 번호 |

응답 201
```json
{
  "userId": 25,
  "email": "user@example.com"
}
```

---

### 3. 회원가입 ‑ 관리자

```
POST /api/v1/auth/register/admin
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | Y | 이메일 |
| password | string | Y | 비밀번호 |
| name | string | Y | 관리자 이름 |
| companyName | string | Y | 회사명 |

응답 201
```json
{
  "userId": 2,
  "companyId": "CMP-20240518-001"
}
```

---

### 4. 회원가입 ‑ 상담원

```
POST /api/v1/auth/register/counselor
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | Y | 이메일 |
| password | string | Y | 비밀번호 |
| name | string | Y | 상담원 이름 |
| inviteCode | string | Y | 초대코드 |

응답 201
```json
{
  "userId": 17,
  "companyId": "CMP-20240518-001"
}
```

---

### 5. 초대코드 생성 (관리자)

```
POST /api/v1/invitations
```

헤더: `X-Auth-Role: ADMIN`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| expiresIn | number | Y | 만료 시간(분) |

응답 201
```json
{
  "code": "ABCD-EFGH",
  "expiresAt": "2024-05-18T12:00:00Z"
}
```

---

### 6. 초대코드 검증

```
GET /api/v1/invitations/{code}
```

응답 200
```json
{
  "valid": true,
  "companyName": "인스웨이브",
  "adminName": "홍길동"
}
```

응답 404
```json
{
  "valid": false,
  "message": "Invite code not found or expired"
}
```

---

### 공통 상태 코드

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 201 | 리소스 생성 완료 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 | 