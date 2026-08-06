# 카트 재연결 및 앱 재실행 복구 기능

## 1. 개요

사용자가 카트를 명시적으로 반납하지 않고 앱만 종료했다가 다시 실행한 경우, 기존 카트와 장바구니를 계속 사용할 수 있도록 하는 기능이다.

기존에는 Redis에 카트 세션이 남아 있으면 요청 사용자가 기존 점유자와 같은지 관계없이 `CART_ALREADY_IN_USE` 오류가 발생했다. 변경 후에는 동일한 사용자의 요청을 재연결로 판단한다.

앱 종료는 카트 연결 해제로 간주하지 않는다. 현재 구현에서는 카트 반납 API를 호출했을 때 실제 연결을 종료한다.

## 2. 주요 기능

### 2.1 동일 사용자 재연결

`POST /api/carts/connect`에서 카트의 현재 세션을 확인하여 다음과 같이 처리한다.

| 카트 세션 | 요청 사용자 | 결과 |
|---|---|---|
| 없음 | 모든 사용자 | 신규 연결 `CREATED` |
| 있음 | 기존 점유자와 동일 | 재연결 `RESUMED` |
| 있음 | 기존 점유자와 다름 | `CART_ALREADY_IN_USE` 오류 |
| 다른 카트를 사용 중 | 동일 사용자 | 중복 점유 방지 오류 |

재연결 시에는 기존 상품을 삭제하지 않으며, 카트 세션·사용자 역인덱스·상품의 TTL을 하나의 Lua 스크립트로 갱신한다. 카트 버전은 TTL이 없다.

### 2.2 현재 카트 복구

앱 재실행 시 사용자의 활성 카트를 찾을 수 있도록 다음 Redis 역방향 인덱스를 사용한다.

```text
cart:session:{qrCode} -> CartSession
user:cart:{userId}    -> qrCode
```

`GET /api/carts/current`는 인증된 사용자의 역방향 인덱스와 실제 카트 세션의 소유자를 모두 검증한다. 유효한 세션이면 TTL을 갱신하고 현재 장바구니 스냅샷을 반환한다.

### 2.3 장바구니 스냅샷 동기화

연결 API와 현재 카트 조회 API의 응답에 전체 장바구니 스냅샷이 포함된다. SSE 구독이 완료되기 전에 발생한 이벤트를 놓치더라도 API 응답으로 화면을 복구할 수 있다.

## 3. API

모든 API는 Access Token 인증이 필요하다.

### 3.1 카트 연결 또는 재연결

```http
POST /api/carts/connect
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "qrCode": "cart_001"
}
```

응답 예시:

```json
{
  "cartId": 1,
  "qrCode": "cart_001",
  "status": "IN_USE",
  "connectionType": "RESUMED",
  "snapshot": {
    "qrCode": "cart_001",
    "version": 12,
    "items": [],
    "totalQuantity": 0,
    "totalPrice": 0
  }
}
```

`connectionType`은 다음 두 값 중 하나다.

- `CREATED`: 새로운 카트 세션 생성
- `RESUMED`: 기존 동일 사용자 세션 재연결

### 3.2 현재 카트 조회

```http
GET /api/carts/current
Authorization: Bearer {accessToken}
```

- 활성 카트가 있으면 `200 OK`와 `CartConnectResponse` 반환
- 활성 카트가 없으면 `204 No Content` 반환

### 3.3 카트 반납

```http
DELETE /api/carts/{qrCode}
Authorization: Bearer {accessToken}
```

반납 시 다음 정보를 정리한다.

- 장바구니 상품
- 카트 세션
- 사용자별 현재 카트 인덱스
카트 사용 여부는 Redis 세션 존재 여부로 판단하므로 DB 점유 상태는 변경하지 않는다.

## 4. Redis 원자적 점유 처리

카트 세션 조회와 생성을 별도로 수행하면 동시 요청에서 두 사용자가 같은 카트를 점유할 수 있다. 이를 방지하기 위해 Redis Lua 스크립트로 다음 판단을 한 번에 수행한다.

```text
세션 없음                     -> CREATED
세션 소유자 == 요청 사용자 -> RESUMED
세션 소유자 != 요청 사용자 -> CART_CONFLICT
사용자가 다른 카트 사용 중     -> USER_CONFLICT
```

`CREATED`와 `RESUMED`에서는 카트 세션, 사용자 인덱스, 장바구니 키의 TTL이 하나의 Lua 실행에서 함께 설정된다.

## 5. SSE 이벤트

| 상황 | 이벤트 이름 |
|---|---|
| 신규 연결 | `cart-init` |
| 재연결 | `cart-resumed` |
| 상품 변경 | `cart-updated` |
| 카트 반납 | `cart-closed` |

`cart-resumed`는 기존 장바구니 스냅샷을 그대로 전달한다. 재연결 시 상품을 비우지 않는다.

## 6. 클라이언트 적용 흐름

```text
앱 실행
  -> 로그인 정보 복구
  -> GET /api/carts/current
      -> 200: snapshot으로 카트 화면 복구
      -> 204: QR 스캔 화면 표시
  -> SSE 티켓 발급 및 재구독
  -> 이후 카트 변경 이벤트 수신
```

클라이언트는 `snapshot.version`보다 낮은 버전의 SSE 이벤트를 무시해야 한다.

## 7. 관련 코드

- `CartConnectionService`: 신규 연결, 재연결, 현재 카트 복구, 반납 처리
- `CartSessionRepository`: Redis 세션·역방향 인덱스·원자적 점유 처리
- `CartController`: `/connect`, `/current`, 반납 API 제공
- `CartConnectResponse`: 연결 종류와 장바구니 스냅샷 반환
- `CartChangeType`: `cart-resumed` SSE 이벤트 정의
