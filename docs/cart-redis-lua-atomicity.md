# 카트 Redis Lua 원자화 스크립트

## 1. 개요

카트 기능은 서로 연관된 Redis 키를 여러 개 사용한다.

```text
cart:session:{qrCode} -> 카트 점유 세션
user:cart:{userId}    -> 사용자가 점유한 카트의 QR 코드
cart:items:{qrCode}   -> 장바구니 상품 Hash
```

이 키들을 개별 Redis 명령으로 조회하거나 변경하면 명령 사이에 다른 요청이 실행되어 상태가 불일치할 수 있다. 이를 방지하기 위해 `CartSessionRepository`는 Lua 스크립트를 `DefaultRedisScript`로 선언하고 `StringRedisTemplate.execute()`로 실행한다.

스크립트는 별도의 `.lua` 파일이 아니라 다음 Java 파일의 텍스트 블록에 포함되어 있다.

```text
src/main/java/com/mart/quickpass/cart/repository/CartSessionRepository.java
```

Redis는 Lua 스크립트가 실행되는 동안 다른 명령을 끼워 넣지 않으므로, 스크립트 안의 조회와 변경은 하나의 원자적 작업으로 처리된다.

## 2. 프로젝트 Redis 저장 구조

QuickPass는 Redis를 영구 원장보다는 만료 가능한 인증 정보, 카트의 실시간 상태, 동시성 제어 데이터에 사용한다. 실제 상품·사용자·주문 등의 영구 데이터는 MySQL에서 관리한다.

### 2.1 전체 키 구조

| 키 패턴 | Redis 자료형 | 값 | TTL | 용도 |
|---|---|---|---|---|
| `refreshToken:{userId}` | String | Refresh Token | 토큰 유효기간, 기본 14일 | 토큰 재발급 및 로그아웃 |
| `emailVerification:code:{email}` | String | 인증번호의 Hash | 기본 10분 | 이메일 인증번호 검증 |
| `emailVerification:verified:{email}` | String | `true` | 기본 30분 | 이메일 인증 완료 여부 |
| `emailVerification:cooldown:{email}` | String | `true` | 기본 1분 | 인증 메일 재발송 제한 |
| `cart:session:{qrCode}` | String | JSON `CartSession` | sliding TTL, 기본 2시간 | 카트 점유자와 연결 시각 관리 |
| `user:cart:{userId}` | String | `qrCode` | sliding TTL, 기본 2시간 | 사용자로 현재 카트 조회 |
| `cart:items:{qrCode}` | Hash | field: `barcode`, value: JSON `CartItem` | sliding TTL, 기본 2시간 | 카트별 장바구니 상품 |
| `cart:version:{qrCode}` | String 숫자 | 변경 버전 | 없음 | SSE 스냅샷과 이벤트 순서 비교 |
| `cart:sse:ticket:{ticket}` | String | `userId` | 30초 | SSE 연결용 일회성 인증 |

표의 TTL은 현재 `application.yml`과 코드의 기본 설정을 기준으로 한다. 운영 환경 설정이 바뀌면 실제 만료 시간도 달라질 수 있다.

### 2.2 카트 데이터의 연결 관계

예를 들어 사용자 `42`가 `cart_001`을 점유하고 생수 한 개를 담았다면 개념적으로 다음과 같이 저장된다.

```text
cart:session:cart_001
  -> {"userId":42,"startedAt":"..."}

user:cart:42
  -> cart_001

cart:items:cart_001
  └─ 8801234567890 -> {"name":"생수","price":1000,"quantity":1}

cart:version:cart_001
  -> 7
```

`cart:session`과 `user:cart`는 같은 점유 관계를 서로 다른 조회 방향으로 표현한다. `cart:items`는 하나의 카트 키 아래 상품 바코드를 Hash field로 두므로 상품 하나를 조회·수정·삭제하거나 전체 장바구니를 읽을 수 있다.

카트 세션, 사용자 역인덱스, 장바구니는 사용자 활동 때 TTL을 다시 기본값으로 갱신하는 sliding TTL 데이터다. 반면 `cart:version`은 세션 수명과 분리된 순서 값이므로 현재 TTL이 없다.

### 2.3 인증 및 이메일 데이터

Refresh Token은 사용자 한 명당 하나의 키로 저장한다. 새 토큰 저장 시 기존 값이 덮어써지며, 로그아웃이나 토큰 회전 시 키를 삭제한다.

이메일 인증은 목적에 따라 키를 분리한다.

- `code`: 원문 인증번호가 아닌 Hash를 저장한다.
- `verified`: 인증을 완료했다는 단기 상태를 저장하고 회원가입 시 소비한다.
- `cooldown`: `SET NX`를 사용해 같은 이메일의 동시 재발송 요청 중 하나만 통과시킨다.

SSE 티켓은 Access Token을 URL에 노출하지 않기 위한 단명 자격 증명이다. 소비할 때 `GETDEL`로 읽기와 삭제를 원자적으로 처리하므로 한 번만 사용할 수 있다.

## 3. 역인덱스의 개념과 적용 이유

### 3.1 역인덱스란

인덱스는 특정 값을 기준으로 원하는 데이터를 빠르게 찾기 위한 별도의 조회 구조다. 역인덱스는 기존 데이터가 제공하는 조회 방향의 반대 방향으로 찾기 위해 만든 인덱스다.

이 프로젝트의 원본 카트 점유 구조는 다음 방향이다.

```text
qrCode -> CartSession -> userId
```

즉, `cart:session:{qrCode}`를 사용하면 QR 코드를 알고 있을 때 점유 사용자를 바로 알 수 있다. 하지만 앱 재실행 시에는 인증된 `userId`만 알고 있고 사용 중인 `qrCode`는 모른다. Redis는 값 내부의 `userId`를 기준으로 String 키 전체를 효율적으로 역조회하지 않으므로, 모든 `cart:session:*` 키를 순회해야 한다.

이를 반대 방향으로 조회하기 위해 다음 키를 추가한다.

```text
userId -> qrCode

user:cart:{userId} -> qrCode
```

`cart:session`의 `qrCode -> userId` 관계를 뒤집어 `userId -> qrCode` 조회를 제공하므로 `user:cart`를 역인덱스라고 부른다.

### 3.2 역인덱스가 필요한 이유

역인덱스가 있으면 `GET user:cart:{userId}` 한 번으로 사용자의 현재 카트를 찾을 수 있다. 따라서 다음 기능을 Redis 키 전체 탐색 없이 처리한다.

- 앱 재실행 후 현재 카트 복구
- 한 사용자의 여러 카트 동시 점유 방지
- 사용자 기준 카트 세션 정리

조회 흐름은 다음과 같다.

```text
userId 42
  -> GET user:cart:42
  -> cart_001
  -> GET cart:session:cart_001
  -> 세션의 userId가 42인지 최종 검증
```

역인덱스는 원본 데이터의 복사본이므로 두 키 중 하나만 갱신되면 불일치가 생길 수 있다. 그래서 신규 점유와 재연결에서는 `CLAIM_SCRIPT`가 `cart:session`과 `user:cart`를 함께 검사하고 변경한다. TTL 갱신도 `REFRESH_TTL_SCRIPT`가 관련 키를 함께 처리한다.

다만 TTL 만료 시점이나 장애 상황에서 한쪽 키만 남을 가능성을 완전히 배제할 수 없으므로, 현재 카트 복구 시 역인덱스 결과만 신뢰하지 않고 실제 `cart:session`의 소유자까지 다시 검증한다.

## 4. 카트 점유 및 재연결 스크립트

### 4.1 목적

`CLAIM_SCRIPT`는 다음 작업을 하나의 Redis 실행으로 처리한다.

- 카트의 기존 점유자 확인
- 사용자가 이미 다른 카트를 점유했는지 확인
- 신규 카트 세션 및 사용자 역인덱스 생성
- 동일 사용자의 카트 재연결
- 세션성 키의 TTL 갱신

조회와 생성을 따로 실행할 때 발생할 수 있는 두 사용자의 동시 점유를 방지한다.

### 4.2 입력

| 구분 | 값 |
|---|---|
| `KEYS[1]` | `cart:session:{qrCode}` |
| `KEYS[2]` | `user:cart:{userId}` |
| `KEYS[3]` | `cart:items:{qrCode}` |
| `ARGV[1]` | 요청 사용자 ID |
| `ARGV[2]` | 요청 카트 QR 코드 |
| `ARGV[3]` | JSON으로 직렬화한 `CartSession` |
| `ARGV[4]` | 밀리초 단위 TTL |

### 4.3 처리 결과

| 반환값 | Repository 결과 | 의미 |
|---:|---|---|
| `0` | `CREATED` | 점유 정보가 없어 새 세션 생성 |
| `1` | `RESUMED` | 동일 사용자가 기존 세션에 재연결 |
| `2` | `CART_CONFLICT` | 다른 사용자가 해당 카트를 점유 중 |
| `3` | `USER_CONFLICT` | 요청 사용자가 다른 카트를 점유 중 |

신규 연결에서는 카트 세션과 사용자 역인덱스를 `SET ... PX`로 생성한다. 장바구니 키는 상품이 추가될 때 생성되므로 신규 연결 과정에서 빈 Hash를 만들지 않는다.

재연결에서는 다음 처리를 수행한다.

```text
카트 세션 PEXPIRE
사용자 역인덱스 SET ... PX
장바구니 상품 PEXPIRE
```

`PEXPIRE` 대상 키가 없으면 Redis는 `0`을 반환하며, 스크립트 실행 자체는 계속된다.

## 5. 세션 TTL 갱신 스크립트

### 5.1 목적

`REFRESH_TTL_SCRIPT`는 사용자 활동이 발생했을 때 세 세션성 키의 sliding TTL을 한 번에 갱신한다.

호출 메서드는 다음과 같다.

```java
refreshSessionTtl(Long userId, String qrCode, Duration ttl)
```

상품 스캔, 수량 변경, 상품 삭제, 현재 카트 조회 등의 흐름에서 이 메서드를 사용한다.

### 5.2 입력과 처리

| 구분 | 값 |
|---|---|
| `KEYS[1]` | `cart:session:{qrCode}` |
| `KEYS[2]` | `user:cart:{userId}` |
| `KEYS[3]` | `cart:items:{qrCode}` |
| `ARGV[1]` | 밀리초 단위 TTL |

스크립트는 모든 `KEYS`를 순회하며 다음 명령을 실행한다.

```lua
redis.call('PEXPIRE', KEYS[i], ARGV[1])
```

반환값은 TTL 갱신에 성공한 키의 개수다. 존재하지 않는 키에 대한 `PEXPIRE`는 `0`을 반환하므로 합계에 포함되지 않는다. 현재 Repository는 반환 개수를 업무 판단에 사용하지 않는다.

## 6. 보장되는 원자성

Lua 스크립트로 보장되는 범위는 각 스크립트 한 번의 실행 내부다.

- 점유 상태 확인과 세션 생성 사이에 다른 Redis 명령이 끼어들지 않는다.
- 관련 세션 키들의 TTL 갱신 사이에 다른 Redis 명령이 끼어들지 않는다.
- 충돌로 판단되면 점유 정보를 변경하지 않고 결과 코드를 반환한다.

다음 작업까지 하나의 트랜잭션으로 묶이는 것은 아니다.

- MySQL 데이터 변경
- MQTT 메시지 처리
- SSE 이벤트 발행
- 장바구니 Hash 변경과 카트 버전 증가
- 서로 다른 Lua 스크립트 호출

따라서 Lua 실행 전후의 애플리케이션 처리에서 장애가 발생하면 별도의 복구 또는 멱등성 전략이 필요하다. 또한 Redis Lua는 실행 중 런타임 오류가 발생했을 때 이미 수행된 쓰기를 데이터베이스 트랜잭션처럼 자동 롤백하지 않는다.

## 7. Redis Cluster 사용 시 주의사항

Redis Cluster에서 여러 키를 받는 Lua 스크립트를 실행하려면 모든 키가 같은 hash slot에 있어야 한다. 현재 키 이름에는 Redis hash tag가 없으므로 Cluster 구성으로 전환할 때는 키 설계를 함께 변경해야 한다.

예시는 다음과 같다.

```text
cart:{cart_001}:session
cart:{cart_001}:items
```

다만 사용자 역인덱스는 카트 QR 코드를 기준으로 동일한 hash tag를 만들기 어려울 수 있으므로, Cluster 전환 시 원자성 범위와 데이터 구조를 다시 설계해야 한다.

## 8. 관련 코드

- `CartSessionRepository.CLAIM_SCRIPT`: 카트 점유 및 재연결
- `CartSessionRepository.claimOrResume()`: 점유 스크립트 실행
- `CartSessionRepository.REFRESH_TTL_SCRIPT`: 세션성 키 TTL 갱신
- `CartSessionRepository.refreshSessionTtl()`: TTL 스크립트 실행
- `CartConnectionService`: 신규 연결, 재연결, 현재 카트 복구
- `CartItemService`: 상품 수량 변경 및 삭제 후 TTL 갱신
- `CartScanService`: 상품 스캔 후 TTL 갱신
- `CartItemsRepository`: 카트별 상품 Hash 저장
- `CartVersionRepository`: 카트 변경 버전 저장
- `RefreshTokenRepository`: Refresh Token 저장
- `EmailVerificationRepository`: 이메일 인증 상태 저장
- `SseTicketService`: SSE 일회성 티켓 저장 및 소비
