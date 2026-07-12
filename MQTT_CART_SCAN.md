# QuickPass 백엔드 구현 기능 문서

마트 무인 결제(셀프 체크아웃) 서비스 백엔드의 구현 기능 설명서입니다.
**① 인증**, **② 카트 점유·반납**, **③ MQTT 스캔 → 장바구니 반영**, **④ 장바구니 아이템 조작**,
**⑤ 상품/데이터 시더**, **⑥ 개발용 가상 카트 시뮬레이터**를 다룹니다.

- **기술 스택**: Java 21, Spring Boot 4.1.0, Spring Security, Spring Data JPA, Redis, MySQL,
  JWT(jjwt), Spring Integration MQTT(Eclipse Paho), springdoc(Swagger UI), Lombok
- **패키지 구조**: 도메인형(`auth`, `user`, `cart`, `order`, `product`) + 공통(`global`)

---

## 목차
1. [인증 기능](#1-인증-기능)
2. [카트 점유(연결) & 반납](#2-카트-점유연결--반납)
3. [MQTT 카트 스캔 → 장바구니 반영](#3-mqtt-카트-스캔--장바구니-반영)
4. [장바구니 아이템 조작 (앱)](#4-장바구니-아이템-조작-앱)
5. [상품(Product) & 개발용 데이터 시더](#5-상품product--개발용-데이터-시더)
6. [개발용 가상 카트 시뮬레이터](#6-개발용-가상-카트-시뮬레이터)
7. [공통 — 예외 처리](#7-공통--예외-처리)
8. [설정 (application.yml)](#8-설정-applicationyml)
9. [실행 & 검증](#9-실행--검증)
10. [다음 단계 (TODO)](#10-다음-단계-todo)

---

## 1. 인증 기능

JWT 기반 **Stateless 인증**. Access Token은 `Authorization` 헤더, Refresh Token은 쿠키 +
**Redis 저장**으로 관리하며, 재발급 시 **토큰 회전(Rotation)** 으로 탈취에 대응한다.

### 1-1. 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
|---|---|---|---|
| `POST` | `/api/users/signup` | 회원가입 | 불필요 |
| `POST` | `/api/auth/login` | 로그인 (토큰 발급) | 불필요 |
| `POST` | `/api/auth/reissue` | 토큰 재발급 (쿠키의 Refresh 사용) | 불필요 |
| `POST` | `/api/auth/logout` | 로그아웃 (Refresh 삭제) | 불필요 |
| 그 외 | `/**` | 모든 요청 | **Access Token 필요** |

### 1-2. 회원가입 (`user` 도메인)
- 이메일 중복 검사(`existsByEmail`) → 중복 시 `DuplicateEmailException`(409)
- 비밀번호를 **BCrypt로 암호화**하여 저장, 응답에는 비밀번호 미포함
- 입력 검증(`SignUpRequest`): 이메일 형식, 비밀번호(영문·숫자·특수문자 **8~20자**), 이름 최대 20자
- 성공 시 `201 Created` + `SignUpResponse(id, email, name)`

### 1-3. 로그인 (`auth` 도메인)
- 이메일 조회 → `PasswordEncoder.matches()`로 비밀번호 검증
- **실패 시 사용자 없음/비밀번호 불일치 모두 동일한 `InvalidCredentialsException`(401)** — 계정 존재 여부 노출 방지
- 성공 시: **Access Token** → `Authorization` 헤더, **Refresh Token** → `Set-Cookie` + **Redis 저장**

### 1-4. 토큰 재발급 & 로그아웃

| 기능 | 동작 |
|---|---|
| **재발급** (`/reissue`) | 쿠키의 Refresh 검증 → Redis 값과 비교 → 일치 시 Access/Refresh 모두 새로 발급(회전). 불일치 시 탈취로 간주해 저장 토큰 삭제 후 `InvalidTokenException`(401) |
| **로그아웃** (`/logout`) | Redis의 Refresh 삭제 + 쿠키 즉시 만료 |

### 1-5. 구성 요소

| 파일 | 역할 |
|---|---|
| `global/security/jwt/JwtTokenProvider` | 토큰 생성/검증 (Access: subject=userId + `email`·`type=access`, Refresh: `type=refresh`) |
| `global/security/jwt/JwtAuthenticationFilter` | 요청마다 토큰 추출 → **Access Token만** 인증에 사용 → `SecurityContext`에 `userId`를 principal로 등록 |
| `global/security/JwtAuthenticationEntryPoint` | 미인증 접근 시 **401을 일관된 JSON**으로 반환 (`EXPIRED_TOKEN` / `INVALID_TOKEN` 구분) |
| `global/config/SecurityConfig` | CSRF 비활성 + 세션 STATELESS, CORS, permitAll 경로, 필터 등록, `BCryptPasswordEncoder` 빈 |
| `auth/repository/RefreshTokenRepository` | Redis에 `refreshToken:{userId}` 저장, TTL = refresh 유효기간 |

> ⚠️ **개발용 임시 설정**: 쿠키 `httpOnly(false)`, CORS 모든 origin 허용 → 배포 전 프론트 도메인으로 축소 필요.
> Swagger UI(`/swagger-ui.html`) 등 springdoc 경로도 permitAll에 열려 있으므로 운영에선 차단/인증 필요.

---

## 2. 카트 점유(연결) & 반납

앱(폰)이 카트에 붙은 **QR 코드**를 스캔해 그 카트를 **점유**하고, 장보기가 끝나면 **반납**하는 유스케이스.
`cart/service/CartConnectionService` + `cart/controller/CartController`가 담당한다.

### 2-1. 엔드포인트 (모두 Access Token 필요)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/carts/connect` | 카트 점유 + 사용자 세션 등록 |
| `DELETE` | `/api/carts/{qrCode}` | 카트 반납 (아이템 전체 비우기 + 점유 해제) |

- **요청**: `POST /connect` 바디 `{"qrCode":"cart_001"}` (`@NotBlank`)
- **응답**: `CartConnectResponse(cartId, qrCode, status)`
- `userId`는 `@AuthenticationPrincipal`로 JWT에서 주입된다.

### 2-2. 점유의 진실 원천은 **Redis 세션** (중요)

점유 여부의 실제 판단 기준은 **Redis 세션**이다. `CartSessionRepository.claim()`이
Redis **`SETNX`**(`setIfAbsent`)로 원자적으로 동작하므로, 동시에 같은 카트를 스캔해도 **단 한 요청만 성공**한다.
DB의 `Cart.status`는 조회/표시용 **보조 필드**로 함께 갱신된다(`markInUse` / `markWaiting`).

```
connect(userId, qrCode):
  1. findByQrCode → 없으면 CartNotFoundException(404)
  2. claim(qrCode, CartSession.start(userId), ttl)  ── SETNX, 실패(이미 점유) 시 CartAlreadyInUseException(409)
  3. cart.markInUse()  ── DB 상태 보조 갱신
  4. → CartConnectResponse

disconnect(userId, qrCode):
  1. CartSessionGuard.requireOwnedSession ── 세션 없으면 404, 남의 카트면 403
  2. cart:items:{qrCode} 전체 삭제  (deleteAll)
  3. cart:session:{qrCode} 삭제
  4. cart.markWaiting()  ── 다른 사용자가 다시 점유 가능
  5. → 204 No Content
```

### 2-3. Redis 세션 스키마

| 항목 | 값 |
|---|---|
| Key | `cart:session:{qrCode}` (String) |
| Value | `CartSession(userId, status=IN_USE, startedAt)` JSON |
| TTL | `cart.session.ttl`(기본 **2h**) — **sliding**: 스캔/아이템 조작 시마다 초기화 |

### 2-4. 구성 요소

| 파일 | 역할 |
|---|---|
| `cart/controller/CartController` | `/api/carts/*` 엔드포인트 |
| `cart/service/CartConnectionService` | 점유/반납 유스케이스 |
| `cart/service/CartSessionGuard` | 요청자 == 세션 점유자 검증 (세션 없음 404 / 타인 403) |
| `cart/repository/CartSessionRepository` | `claim`(SETNX)·조회·삭제·`refreshTtl` |
| `cart/dto/CartSession` | 세션 record (`start(userId)` 팩토리) |
| `cart/dto/CartConnectRequest` / `CartConnectResponse` | 요청/응답 DTO |

---

## 3. MQTT 카트 스캔 → 장바구니 반영

ESP32 카트(또는 [시뮬레이터](#6-개발용-가상-카트-시뮬레이터))가 스캔한 바코드를 MQTT로 수신해
**해당 카트를 점유한 사용자의 장바구니(Redis)에 상품을 담는다**.

### 3-1. 아키텍처

```
카트(ESP32/시뮬레이터)      Mosquitto            Spring Boot 서버
    │  publish                │                      │
    │  quickpass/cart/        │  subscribe           │
    │  {qrCode}/scan   ─────▶  │  quickpass/cart/+/scan ─▶ MqttPahoMessageDrivenChannelAdapter
    │  {"barcode":"..."}      │                      │      │ (mqttInboundChannel)
                                                     │      ▼
                                                     │  CartScanMqttSubscriber  ← 토픽에서 qrCode 추출 + JSON 파싱
                                                     │      ▼
                                                     │  CartScanService         ← 카트/세션/상품 검증 후 장바구니 갱신
                                                     │      ▼
                                                     │  Redis (cart:items:{qrCode})
```

- **설계 원칙**: MQTT 프로토콜 지식은 `global/mqtt`에만 존재. 도메인 계층(`cart`)은 프로토콜을 모른다.

### 3-2. 스캔 처리 로직 (`CartScanService.handleScan`)

```
1. 카트 존재?          ── findByQrCode 없으면  → warn "등록되지 않은 카트" 후 무시
2. 점유 세션 존재?     ── cart:session 없으면  → warn "연동되지 않은 카트의 스캔" 후 무시
3. 상품 존재?          ── findByBarcode 없으면 → warn "등록되지 않은 상품 바코드" 후 무시
4. 장바구니에 담기      ── 있으면 수량 +1, 없으면 quantity=1로 신규
5. sliding TTL 초기화  ── 세션·아이템 세션 둘 다 refreshTtl
```
잘못된/미연동 스캔은 예외를 던지지 않고 **로그만 남기고 무시**해 수신 스레드를 보호한다.

### 3-3. 토픽 / 페이로드 명세

| 항목 | 값 |
|---|---|
| 발행 토픽 | `quickpass/cart/{qrCode}/scan` (예: `quickpass/cart/cart_001/scan`) |
| 구독 토픽 | `quickpass/cart/+/scan` (`+` = qrCode 와일드카드) |
| Payload | `{"barcode":"<바코드>", "scannedAt":<epoch millis, 선택>}` |

### 3-4. 장바구니 아이템 Redis 스키마

| 항목 | 값 |
|---|---|
| Key | `cart:items:{qrCode}` (**Hash**) |
| Field | `barcode` |
| Value | `CartItem(name, price, quantity)` JSON |
| TTL | 세션과 동일(2h, sliding) |

### 3-5. 구성 요소

| 파일 | 역할 |
|---|---|
| `global/mqtt/MqttProperties` / `MqttConfig` | `mqtt.*` 바인딩 + 클라이언트 팩토리·인바운드 채널·구독 어댑터 |
| `global/mqtt/CartScanMqttSubscriber` | 채널 소비. 토픽에서 qrCode 추출 + payload JSON 파싱 → 서비스 위임 (예외 방어 warn) |
| `cart/dto/CartScanMessage` | 스캔 payload record(`barcode`, `scannedAt`) |
| `cart/service/CartScanService` | 카트/세션/상품 검증 → 장바구니 갱신 → TTL 초기화 |
| `cart/repository/CartItemsRepository` | `cart:items:{qrCode}` Hash CRUD + `deleteAll` + `refreshTtl` |

---

## 4. 장바구니 아이템 조작 (앱)

앱에서 담긴 상품의 **수량을 조절하거나 개별 삭제**한다. 두 동작 모두 `CartSessionGuard`로
**요청자가 현재 점유자인지 먼저 확인**한다(세션 없음 404 / 타인 403).

### 4-1. 엔드포인트 (모두 Access Token 필요)

| 메서드 | 경로 | 설명 | 응답 |
|---|---|---|---|
| `PATCH` | `/api/carts/{qrCode}/items/{barcode}` | 수량 증감 (바디 `{"delta": 1}` / `{"delta": -1}`) | 남으면 `200` + `CartItemResponse`, 0 이하로 제거되면 `204` |
| `DELETE` | `/api/carts/{qrCode}/items/{barcode}` | 해당 상품 완전 삭제 | `204` |

- **수량 증감**: 대상 아이템 없으면 `CartItemNotFoundException`(404). `현재수량 + delta <= 0`이면 아이템 제거 후 `204`.
- 두 동작 모두 완료 후 세션·아이템 **sliding TTL 초기화**.
- `CartItemService` 담당. `CartItemAdjustRequest.delta`는 `@NotNull`.

---

## 5. 상품(Product) & 개발용 데이터 시더

### 5-1. 상품 엔티티 (`product` 도메인)

| 파일 | 역할 |
|---|---|
| `product/entity/Product` | `barcode`(unique), `name`, `price`, `category`, `status` |
| `product/entity/ProductStatus` | `ON_SALE` / `SOLD_OUT` |
| `product/repository/ProductRepository` | `existsByBarcode`(시드 멱등성), `findByBarcode`(스캔 처리 시 조회) |

### 5-2. 개발용 가상 데이터 시더 (`global/init/DevDataInitializer`)

- `CommandLineRunner`로 기동 후 1회 실행, `ddl-auto: create`로 매 기동 시 테이블이 비므로 뜰 때마다 자동 시드
- `existsByQrCode` / `existsByBarcode`로 **멱등성** 보장
- ⚠️ **현재 프로필 제한 없이 모든 환경에서 실행됨**. 배포 전 `@Profile("local")` 복구 필요 (코드에 TODO 표기)

**시드 데이터**

| 종류 | 값 |
|---|---|
| 카트 | `Cart { qrCode: "cart_001", status: WAITING }` |
| 상품 | `라면` — barcode `8801234567890`, 1200원, 식품 |
| 상품 | `샴푸` — barcode `8801234567891`, 9800원, 생활용품 |
| 상품 | `삼겹살` — barcode `8801234567892`, 15000원, 정육 |

> 라면 바코드(`8801234567890`)는 문서/시뮬레이터의 기본 예시 바코드와 일치시켜 두었다.

---

## 6. 개발용 가상 카트 시뮬레이터

실제 ESP32 카트 하드웨어가 없는 동안 **카트의 역할(바코드 스캔을 MQTT로 발행)** 을 대신하는 개발 도구.

> ⚠️ 이 도구는 **로컬 전용**이라 `.gitignore`로 버전 관리에서 제외되어 있다(`tools/cart-simulator/`).
> 클론한 환경에는 파일이 없으므로 아래 스펙대로 새로 만들어 사용한다.

- **위치**: `tools/cart-simulator/` (`cart_simulator.py`, `requirements.txt`, `README.md`)
- **언어/의존성**: Python 3 + `paho-mqtt` (venv에 설치)
- **역할 범위**: **카트 역할 전용** — 브로커 접속 + 스캔 발행만. 폰의 점유(`POST /connect`)는 범위 밖.
- **입력 방식**: 타이핑 명령형 REPL. 시작 시 자동으로 `tcp://localhost:1883` 접속.

**명령어**

| 명령 | 설명 |
|---|---|
| `scan [barcode]` | 스캔 발행. barcode 생략 시 기본값(라면 `8801234567890`) |
| `cart <qrCode>` | 흉내낼 카트 전환 (예: `cart cart_002`) |
| `connect` / `disconnect` | 브로커 접속/해제 (카트 전원 ON/OFF) |
| `bad` | 깨진 JSON 발행 (서버 방어 로직 테스트) |
| `products` | 시더에 심어둔 상품 바코드 목록 |
| `help` / `quit` | 도움말 / 종료 |

---

## 7. 공통 — 예외 처리

`global/exception/GlobalExceptionHandler`(`@RestControllerAdvice`)가 예외를 일관된
`ErrorResponse(message, details)` JSON으로 변환한다.

| 예외 | HTTP | 발생 상황 |
|---|---|---|
| `DuplicateEmailException` | 409 | 회원가입 이메일 중복 |
| `InvalidCredentialsException` | 401 | 로그인 실패(사용자 없음/비번 불일치) |
| `InvalidTokenException` | 401 | Refresh 토큰 무효/불일치 |
| `CartNotFoundException` | 404 | 존재하지 않는 카트 |
| `CartAlreadyInUseException` | 409 | 이미 점유 중인 카트 연결 시도 |
| `CartSessionNotFoundException` | 404 | 연동(점유)되지 않은 카트 조작 |
| `CartAccessDeniedException` | 403 | 본인이 연결하지 않은 카트 조작 |
| `CartItemNotFoundException` | 404 | 장바구니에 없는 상품 조작 |
| `MethodArgumentNotValidException` | 400 | `@Valid` 검증 실패(필드별 메시지 목록 포함) |

> 인증 필터 단계의 미인증 접근(401)은 `JwtAuthenticationEntryPoint`가 별도 처리한다.

---

## 8. 설정 (application.yml)

```yaml
mqtt:
  broker-url: tcp://localhost:1883    # Mosquitto 브로커 주소
  client-id: quickpass-server         # MQTT 클라이언트 ID (연결마다 고유)
  scan-topic: quickpass/cart/+/scan   # 구독 토픽 (+ = qrCode 와일드카드)
  qos: 1                              # 0=최대1회, 1=최소1회, 2=정확히1회

cart:
  session:
    ttl: 2h    # 카트 점유/아이템 세션의 유휴 타임아웃 (스캔/연동 시마다 초기화)
```

- `@ConfigurationPropertiesScan`(메인 클래스)으로 `MqttProperties`·`CartSessionProperties`·`JwtProperties` 바인딩.
- **인프라(docker-compose)**: `mysql:8.0`(3306), `redis:7.2`(6379), `eclipse-mosquitto:2.0`(1883 MQTT / 9001 WS).
  Mosquitto는 개발용으로 `allow_anonymous true`.

---

## 9. 실행 & 검증

### 1) 인프라 기동
```bash
docker compose up -d
```

### 2) 앱 실행
```bash
./gradlew bootRun
```
IntelliJ에서 그냥 Run 해도 됨(현재 시더가 프로필 무관하게 실행됨).
기동 로그: `[DevData] 가상 카트 시드 완료 ...` + `[DevData] 상품 시드 완료 ...` ×3
API 문서: `http://localhost:8080/swagger-ui.html`

### 3) 인증 → 카트 점유
```bash
# 회원가입
curl -s -X POST http://localhost:8080/api/users/signup \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@mart.com","password":"test1234!","name":"홍길동"}'

# 로그인 → 응답 헤더 Authorization 토큰 확보
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@mart.com","password":"test1234!"}'

# 카트 점유 (<TOKEN> = 위 Authorization 값)
curl -i -X POST http://localhost:8080/api/carts/connect \
  -H 'Authorization: Bearer <TOKEN>' -H 'Content-Type: application/json' \
  -d '{"qrCode":"cart_001"}'
```
→ `200` + `{"cartId":1,"qrCode":"cart_001","status":"IN_USE"}`

### 4) 카트 스캔 (하드웨어 대체)

**(A) 시뮬레이터** — [6절](#6-개발용-가상-카트-시뮬레이터) 참고
```
[cart_001]> scan            # 라면 담기
[cart_001]> scan 8801234567892   # 삼겹살 담기
```

**(B) mosquitto_pub** — 단발성
```bash
docker exec quickpass-mosquitto mosquitto_pub \
  -t 'quickpass/cart/cart_001/scan' \
  -m '{"barcode":"8801234567890","scannedAt":1720000000000}'
```
정상 로그: `[CartScan] qrCode=cart_001, barcode=8801234567890, scannedAt=...`
- 미연동 카트(점유 안 함) → `연동되지 않은 카트의 스캔` warn
- 없는 상품 바코드 → `등록되지 않은 상품 바코드` warn
- 깨진 JSON → `[Mqtt] 스캔 메시지 처리 실패` warn

### 5) 장바구니 아이템 조작 / 반납
```bash
# 라면 수량 +1
curl -i -X PATCH http://localhost:8080/api/carts/cart_001/items/8801234567890 \
  -H 'Authorization: Bearer <TOKEN>' -H 'Content-Type: application/json' -d '{"delta":1}'

# 라면 삭제
curl -i -X DELETE http://localhost:8080/api/carts/cart_001/items/8801234567890 \
  -H 'Authorization: Bearer <TOKEN>'

# 카트 반납 (아이템 전체 비우기 + 점유 해제)
curl -i -X DELETE http://localhost:8080/api/carts/cart_001 \
  -H 'Authorization: Bearer <TOKEN>'
```

---

## 10. 다음 단계 (TODO)

1. **SSE 실시간 반영** — 스캔으로 장바구니가 바뀌면 사용자별 `SseEmitter`로 push (현재는 앱이 조회로만 확인).
2. **장바구니 조회 API** — `GET /api/carts/{qrCode}/items` (현재 조작만 있고 조회 엔드포인트 없음).
3. **결제/주문 연동** — `order` 도메인과 연결, 반납 대신 "결제 완료"로 세션 종료.
4. **운영 보안** — 쿠키 `httpOnly=true`, CORS·Swagger 경로 축소, Mosquitto 인증(`allow_anonymous false`),
   다중 인스턴스 시 MQTT `client-id` 고유화.
