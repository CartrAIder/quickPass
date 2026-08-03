# AGENTS.md

## 프로젝트 개요

QuickPass는 오프라인 매장의 스마트 카트 기반 무인 결제 시스템 백엔드이다.

주요 기능은 회원 인증, 카트 연결 및 상품 스캔, 주문 생성, 결제 처리, 관리자 기능을 제공한다.

---

## 기술 스택

- Java 21
- Spring Boot
- Spring Security + JWT
- Spring Data JPA (Hibernate)
- MySQL
- Redis
- MQTT
- Server-Sent Events (SSE)
- Gradle

---

## 프로젝트 구조

소스 코드는 기능(Feature) 단위로 구성한다.

```
src/main/java/com/mart/quickpass
├── auth
├── cart
├── email
├── order
├── payment
├── product
├── user
└── global
```

각 기능은 다음과 같은 구조를 따른다.

```
controller
service
repository
entity
dto
```

공통 기능(보안, 예외 처리, 설정 등)은 `global` 패키지에서 관리한다.

---

## 개발 규칙

### 계층 구조

다음 계층 구조를 유지한다.

```
Controller
    ↓
Service
    ↓
Repository
```

- Controller는 HTTP 요청/응답만 처리한다.
- Service는 비즈니스 로직과 트랜잭션을 담당한다.
- Repository는 데이터 조회 및 저장만 담당한다.

---

### DTO 사용

- Entity를 API 응답으로 직접 반환하지 않는다.
- 요청은 `*Request`
- 응답은 `*Response`
- DTO를 통해 API를 구성한다.

---

### 비즈니스 상태

비즈니스 상태는 Enum으로 관리한다.

예시

- `OrderStatus`
- `PaymentStatus`
- `ProductStatus`

문자열 상수를 직접 사용하지 않는다.

---

### 트랜잭션

- 조회 서비스는 `@Transactional(readOnly = true)`를 사용한다.
- 데이터 변경 작업만 일반 `@Transactional`을 사용한다.

---

## 코딩 스타일

- Java 표준 네이밍을 따른다.
- 클래스는 PascalCase
- 메서드와 변수는 camelCase
- Enum은 UPPER_SNAKE_CASE

불필요한 코드와 사용하지 않는 import는 제거한다.

---

## 테스트

변경 사항에는 가능한 한 테스트를 추가한다.

주요 명령어

```bash
./gradlew test
./gradlew build
```

---

## 보안

다음 정보는 저장소에 포함하지 않는다.

- JWT Secret
- API Key
- 이메일 인증 키
- 결제 키
- 환경 변수(.env)

---

## 문서

프로젝트 설계 및 기능 문서는 `docs/` 디렉터리에서 관리한다.

기존 기능을 수정하거나 새로운 기능을 추가하기 전에는 관련 문서를 먼저 확인한다.