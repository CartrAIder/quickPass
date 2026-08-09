# 사용자(User) 기능 구현 정리

## 1. 개요

`user` 기능은 사용자 계정 정보와 회원가입을 담당한다. 로그인, 비밀번호 변경과 JWT 발급은 `auth` 기능, 가입 전 이메일 인증은 `email` 기능이 담당한다.

소스 위치는 `src/main/java/com/mart/quickpass/user`이며 다음 계층으로 구성되어 있다.

```text
user
├── controller
│   └── UserController
├── service
│   └── UserService
├── repository
│   └── UserRepository
├── entity
│   ├── User
│   └── UserRole
└── dto
    └── SignUpRequest
```

## 2. 계층별 구현

### 2.1 Controller

`UserController`는 `/api/users`를 기본 경로로 사용한다.

- 요청 본문을 `SignUpRequest`로 변환한다.
- `@Valid`를 통해 입력값을 검증한다.
- 회원가입 처리는 `UserService`에 위임한다.
- 가입 성공 시 응답 본문 없이 `201 Created`를 반환한다.

### 2.2 Service

`UserService.signUp()`은 일반 `@Transactional`이 적용된 회원가입 비즈니스 로직이다.

처리 순서는 다음과 같다.

```text
이메일 중복 확인
  -> 이메일 인증 완료 여부 확인
  -> 비밀번호 BCrypt 암호화
  -> USER 권한으로 User 생성
  -> MySQL에 사용자 저장
  -> Redis의 이메일 인증 완료 상태 소비
  -> 처리 종료
```

주요 의존성은 다음과 같다.

| 의존성 | 역할 |
|---|---|
| `UserRepository` | 이메일 중복 확인 및 사용자 저장 |
| `PasswordEncoder` | 평문 비밀번호를 BCrypt 해시로 변환 |
| `EmailVerificationService` | 가입 전 이메일 인증 확인 및 인증 상태 소비 |

가입 권한은 요청으로 받지 않고 항상 `UserRole.USER`로 설정하므로, 회원가입 요청을 통한 관리자 권한 획득을 방지한다.

### 2.3 Repository

`UserRepository`는 `JpaRepository<User, Long>`를 상속하며 기본 CRUD 외에 다음 조회를 제공한다.

| 메서드 | 용도 |
|---|---|
| `existsByEmail(String email)` | 회원가입 및 인증번호 발송 전 이메일 중복 확인 |
| `findByEmail(String email)` | 로그인 시 이메일로 사용자 조회 |

### 2.4 Entity

`User`는 `users` 테이블에 매핑된다.

| 필드 | 타입 | 매핑 및 의미 |
|---|---|---|
| `id` | `Long` | 기본 키, `IDENTITY` 자동 생성 |
| `email` | `String` | 필수, 유일 값 |
| `password` | `String` | 필수, 암호화된 비밀번호 저장 |
| `name` | `String` | 필수 사용자 이름 |
| `role` | `UserRole` | 필수 권한, Enum 이름을 문자열로 저장 |
| `createdAt` | `LocalDateTime` | Hibernate가 생성 시각을 자동 기록, 수정 불가 |
| `updatedAt` | `LocalDateTime` | Hibernate가 최근 수정 시각을 자동 기록 |

기본 생성자는 JPA 사용을 위해 `protected`로 제한하고, 사용자 생성에는 Lombok 빌더를 사용한다.

`UserRole`은 다음 두 상태를 가진다.

- `USER`: 일반 사용자
- `ADMIN`: 관리자

## 3. 회원가입 API

회원가입 API는 Spring Security의 `permitAll` 대상이므로 Access Token 없이 호출할 수 있다. 단, 같은 이메일로 이메일 인증을 먼저 완료해야 한다.

### 3.1 요청

```http
POST /api/users/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password1!",
  "name": "홍길동"
}
```

요청 필드 검증 규칙은 다음과 같다.

| 필드 | 규칙 |
|---|---|
| `email` | 필수, 공백 불가, 올바른 이메일 형식 |
| `password` | 필수, 영문·숫자·특수문자(`!@#$%^&*()_+-=`)를 각각 하나 이상 포함한 8~20자 |
| `name` | 필수, 공백 불가, 최대 20자 |

### 3.2 성공 응답

```http
HTTP/1.1 201 Created
```

성공 응답에는 본문을 포함하지 않는다. 사용자 ID와 권한 같은 내부 정보 및 요청에서 이미 전달받은 이메일과 이름을 불필요하게 다시 노출하지 않는다.

### 3.3 오류 응답

오류는 공통 `ErrorResponse` 형식으로 반환된다.

```json
{
  "code": "DUPLICATE_EMAIL",
  "message": "이미 사용 중인 이메일입니다: user@example.com",
  "details": []
}
```

| 상황 | HTTP 상태 | 오류 코드 |
|---|---:|---|
| 이미 가입된 이메일 | `409 Conflict` | `DUPLICATE_EMAIL` |
| 이메일 인증을 하지 않았거나 인증이 만료됨 | `400 Bad Request` | `EMAIL_NOT_VERIFIED` |
| 요청 필드 검증 실패 | `400 Bad Request` | `VALIDATION_ERROR` |
| JSON 문법 또는 타입 오류 | `400 Bad Request` | `MALFORMED_REQUEST` |

검증 실패 시 `details`에는 각 필드의 검증 메시지가 포함된다.

## 4. 다른 기능과의 연동

### 4.1 로그인 사용자 비밀번호 변경

비밀번호 변경은 유효한 Access Token으로 인증된 사용자만 호출할 수 있다. 현재 비밀번호를 확인한 뒤 새 비밀번호를 BCrypt로 암호화하여 MySQL에 반영하고, Redis에 저장된 기존 Refresh Token을 새 토큰으로 교체한다.

처리 순서는 다음과 같다.

```text
Access Token에서 사용자 ID 확인
  -> 사용자 조회
  -> 현재 비밀번호 검증
  -> 새 비밀번호 BCrypt 암호화 및 Entity 변경
  -> 기존 Refresh Token 삭제
  -> 새 Access/Refresh Token 발급
  -> 새 Refresh Token을 Redis에 저장
  -> 새 토큰을 클라이언트에 반환
```

요청 본문은 웹과 모바일에서 동일하다.

```json
{
  "currentPassword": "Password1!",
  "newPassword": "ChangedPassword1!"
}
```

새 비밀번호는 회원가입 비밀번호와 동일하게 영문·숫자·특수문자를 포함한 8~20자여야 한다.

#### 웹 API

```http
POST /api/auth/password
Authorization: Bearer {currentAccessToken}
Content-Type: application/json
```

성공 시 `200 OK`의 빈 본문을 반환한다.

- 새 Access Token: `Authorization: Bearer {newAccessToken}` 응답 헤더
- 새 Refresh Token: HttpOnly, `SameSite=Strict` 쿠키

#### 모바일 API

```http
POST /api/mobile/auth/password
Authorization: Bearer {currentAccessToken}
Content-Type: application/json
```

성공 시 `200 OK`와 새 토큰 정보를 본문으로 반환한다.

```json
{
  "accessToken": "new-access-token",
  "refreshToken": "new-refresh-token",
  "accessTokenExpiresIn": 1800,
  "refreshTokenExpiresIn": 1209600,
  "tokenType": "Bearer",
  "name": "홍길동"
}
```

현재 비밀번호가 다르면 `400 Bad Request`와 `CURRENT_PASSWORD_MISMATCH` 오류를 반환하며 비밀번호와 토큰은 변경하지 않는다. 새 Refresh Token은 기존 Redis 값을 대체하므로 기존 Refresh Token으로는 더 이상 재발급할 수 없다. 기존 Access Token은 서버에 저장하지 않는 JWT이므로 만료 전까지 서버에서 즉시 폐기되지는 않으며, 클라이언트가 성공 응답을 받는 즉시 새 Access Token으로 교체해야 한다.

### 4.2 비밀번호 재설정

비밀번호를 잊은 사용자는 가입 이메일 인증을 거쳐 비밀번호를 재설정한다. 세 API는 로그인하지 않은 사용자도 호출할 수 있다.

#### 1단계: 인증번호 발송

```http
POST /api/auth/password-reset/code
Content-Type: application/json

{
  "email": "user@example.com"
}
```

가입 여부 노출을 막기 위해 존재하는 이메일과 존재하지 않는 이메일 모두 `200 OK`와 같은 메시지를 반환한다.

```json
{
  "message": "가입된 이메일이라면 인증번호를 전송했습니다."
}
```

두 경우 모두 이메일별 재발송 cooldown을 획득하므로 반복 요청의 응답으로도 가입 여부를 구분할 수 없다. 가입된 이메일인 경우에만 6자리 인증번호를 실제로 발송하고 해시를 Redis에 10분 동안 저장한다. cooldown 중 다시 요청하면 `429 Too Many Requests`와 `PASSWORD_RESET_TOO_FREQUENT`를 반환한다.

#### 2단계: 인증번호 확인

```http
POST /api/auth/password-reset/confirm
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456"
}
```

인증번호 해시가 일치하면 Redis Lua 연산으로 인증번호를 삭제하고 256비트 난수 기반의 일회용 resetToken을 발급한다. 클라이언트에는 원문을 반환하고 Redis에는 SHA-256 해시만 5분간 저장한다.

```json
{
  "resetToken": "one-time-reset-token",
  "expiresIn": 300
}
```

| 상황 | HTTP 상태 | 오류 코드 |
|---|---:|---|
| 인증번호 만료 또는 이미 소비됨 | `400 Bad Request` | `PASSWORD_RESET_CODE_EXPIRED` |
| 인증번호 불일치 | `400 Bad Request` | `INVALID_PASSWORD_RESET_CODE` |

#### 3단계: 새 비밀번호 설정

```http
POST /api/auth/password-reset
Content-Type: application/json

{
  "resetToken": "one-time-reset-token",
  "newPassword": "ChangedPassword1!"
}
```

새 비밀번호를 BCrypt로 암호화해 MySQL에 반영한 뒤 Redis Lua 연산으로 resetToken을 조회·삭제한다. 같은 토큰을 사용한 요청이 동시에 들어와도 한 요청만 소비에 성공하며, 소비에 실패한 요청의 DB 트랜잭션은 롤백된다. 성공하면 해당 사용자의 기존 Refresh Token을 Redis에서 삭제하고 `200 OK`의 빈 본문을 반환한다.

유효하지 않거나 만료·소비된 토큰에는 `400 Bad Request`와 `INVALID_PASSWORD_RESET_TOKEN`을 반환한다. 완료 후 비밀번호 변경 안내 이메일은 DB 커밋 이후 발송되며, 사용자는 로그인 화면에서 새 비밀번호로 다시 로그인해야 한다.

Redis 키는 회원가입 이메일 인증과 분리한다.

```text
passwordReset:cooldown:{email} -> 발송 제한
passwordReset:code:{email}     -> 인증번호 SHA-256 해시
passwordReset:token:{tokenHash} -> 이메일
```

### 4.3 Email

회원가입 전에 `email` 기능을 통해 인증번호를 발송하고 검증해야 한다. 인증 성공 상태는 Redis에 제한된 시간 동안 저장된다.

`UserService`는 저장 전에 `requireVerified(email)`로 상태를 확인하고, 사용자 저장 후 `consumeVerification(email)`으로 인증 완료 상태를 제거해 재사용을 막는다.

### 4.4 Auth

`AuthService`는 `UserRepository.findByEmail()`로 사용자를 조회하고 `PasswordEncoder.matches()`로 비밀번호를 비교한다. 인증 성공 시 사용자 ID와 `UserRole`을 기반으로 JWT를 발급한다.

### 4.5 Security

- `/api/users/signup`은 비인증 접근이 허용된다.
- `/api/auth/password-reset` 하위 API는 비인증 접근이 허용된다.
- 비밀번호 암호화에는 `BCryptPasswordEncoder` 빈을 사용한다.
- 관리자 API인 `/api/admin/**`는 JWT의 `ADMIN` 권한을 요구한다.

## 5. 구현 시 유의사항

- 이메일 중복 여부는 서비스에서 먼저 검사하고, DB의 `unique` 제약도 함께 사용한다.
- 비밀번호 원문은 Entity나 응답 DTO에 노출하지 않는다.
- 비밀번호 변경 시 현재 비밀번호가 일치한 경우에만 비밀번호와 토큰을 교체한다.
- 비밀번호 재설정 인증번호와 resetToken은 각각 한 번만 소비할 수 있다.
- resetToken 원문은 Redis에 저장하지 않고 SHA-256 해시로 조회한다.
- 회원가입 성공 응답에는 사용자 정보를 포함하지 않는다.
- 일반 회원가입의 권한은 서버에서 `USER`로 고정한다.
- 이메일 인증 상태는 사용자 저장 이후 소비된다.
- `AuthServiceTest`에서 로그인 사용자 비밀번호 변경을, `PasswordResetServiceTest`에서 재설정 코드 발송·검증·토큰 소비 흐름을 검증한다. 회원가입 로직 변경 시 정상 가입, 이메일 중복, 미인증 이메일, 입력값 검증 테스트도 추가하는 것이 좋다.

## 6. 관련 코드

- `UserController`: 회원가입 HTTP 요청 및 응답 처리
- `UserService`: 중복 확인, 이메일 인증 확인, 비밀번호 암호화, 사용자 저장
- `UserRepository`: 이메일 기반 존재 여부 및 사용자 조회
- `User`: 사용자 JPA Entity
- `UserRole`: 일반 사용자와 관리자 권한 Enum
- `SignUpRequest`: 회원가입 입력 및 검증 규칙
- `EmailVerificationService`: 이메일 인증 상태 확인 및 소비
- `ChangePasswordRequest`: 현재 비밀번호 및 새 비밀번호 입력과 검증 규칙
- `AuthService`: 로그인, 비밀번호 변경, 기존 Refresh Token 무효화 및 새 토큰 발급
- `PasswordResetController`: 비밀번호 재설정 3단계 공개 API
- `PasswordResetService`: 인증번호 발송, resetToken 발급 및 비밀번호 재설정
- `PasswordResetRepository`: 재설정 상태와 원자적 소비 Lua 연산
- `PasswordResetCompletedEventListener`: DB 커밋 후 변경 안내 이메일 발송
- `SecurityConfig`: 회원가입 공개 경로 및 비밀번호 인코더 설정
