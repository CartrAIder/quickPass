# QuickPass 예외 처리 가이드

이 문서는 현재 QuickPass 서버가 오류를 HTTP 응답으로 바꾸는 방식을 코드 흐름에 맞춰 설명한다. 핵심은 **서비스가 업무 의미를 담은 예외를 던지고**, `GlobalExceptionHandler`가 이를 **일관된 JSON 응답과 HTTP 상태 코드**로 변환한다는 것이다.

## 한눈에 보는 흐름

```text
클라이언트 요청
  → Spring Security 필터(JWT 검사)
  → Controller(@Valid로 요청 형식 검사)
  → Service(업무 규칙 검사)
  → 예외 발생
  → GlobalExceptionHandler(@RestControllerAdvice)
  → ErrorResponse JSON + HTTP 상태 코드
```

예를 들어 존재하지 않는 주문을 결제하려고 하면 다음 순서로 진행된다.

1. `PaymentService.findOwnedOrder()`가 `orderRepository.findByOrderId()`로 주문을 찾는다.
2. 주문이 없으면 `OrderNotFoundException`을 던진다.
3. `GlobalExceptionHandler.handleOrderOrPaymentAttemptNotFound()`가 이 예외를 받는다.
4. 서버는 `404 Not Found`와 아래 형태의 JSON을 돌려준다.

```json
{
  "message": "주문을 찾을 수 없습니다.",
  "details": []
}
```

## 1. 오류 응답 형식: `ErrorResponse`

구현 위치: `src/main/java/com/mart/quickpass/global/exception/ErrorResponse.java`

```java
public record ErrorResponse(String code, String message, List<String> details) { ... }
```

| 필드 | 의미 | 예시 |
| --- | --- | --- |
| `code` | 프론트가 오류 원인을 분기할 때 쓰는 기계용 코드 | `EXPIRED_TOKEN` |
| `message` | 사용자 또는 개발자에게 보여 줄 오류 설명 | `인증이 필요합니다.` |
| `details` | 입력값 검증처럼 여러 상세 오류가 생길 때의 목록 | `["이메일 형식이 올바르지 않습니다."]` |

모든 API 오류는 `code`를 포함한다. 성공 응답은 오류 응답이 아니므로 `code`가 없을 수 있다. `@JsonInclude(NON_NULL)` 설정 때문에 `code`가 없을 때는 JSON에서 아예 빠진다.

## 2. 업무 예외: 서비스에서 던지고 Advice에서 변환

각 업무 예외는 `BusinessException`을 상속한 짧은 클래스다. `BusinessException` 생성자에서 `ErrorCode`를 반드시 받으므로, 예외를 만들 때 코드와 HTTP 상태를 빠뜨릴 수 없다. `GlobalExceptionHandler`는 예외에 담긴 코드로 HTTP 상태와 응답 본문을 만든다.

구현 위치: `src/main/java/com/mart/quickpass/global/exception/GlobalExceptionHandler.java`

### 현재 예외와 HTTP 상태 코드

| 상황 | 예외 | `code` | HTTP 상태 | 이유 |
| --- | --- | ---: | --- |
| 이미 가입한 이메일 | `DuplicateEmailException` | `DUPLICATE_EMAIL` | 409 | 같은 리소스가 이미 존재 |
| 다른 사용자가 카트를 사용 중 | `CartAlreadyInUseException` | `CART_ALREADY_IN_USE` | 409 | 현재 상태와 충돌 |
| 결제/주문 상태가 잘못됨 | `InvalidPaymentStateException` | `INVALID_PAYMENT_STATE` | 409 | 요청은 형식상 맞지만 현재 상태에서 불가 |
| 로그인 정보가 틀림 | `InvalidCredentialsException` | `INVALID_CREDENTIALS` | 401 | 인증 실패 |
| 애플리케이션에서 무효 리프레시 토큰으로 판단 | `InvalidTokenException` | `INVALID_REFRESH_TOKEN` | 401 | 인증 실패 |
| 카트·상품·사용자 없음 | 각 `*NotFoundException` | 리소스별 `*_NOT_FOUND` | 404 | 대상 리소스 없음 |
| 주문 또는 결제 시도 없음 | `OrderNotFoundException`, `PaymentAttemptNotFoundException` | `ORDER_NOT_FOUND`, `PAYMENT_ATTEMPT_NOT_FOUND` | 404 | 대상 리소스 없음 |
| 다른 사람의 카트/주문 접근 | `CartAccessDeniedException`, `OrderAccessDeniedException` | `CART_ACCESS_DENIED`, `ORDER_ACCESS_DENIED` | 403 | 인증은 됐지만 권한 없음 |
| 한 주문에 같은 상품을 중복 전달 | `DuplicateOrderProductException` | `DUPLICATE_ORDER_PRODUCT` | 400 | 요청 내용이 잘못됨 |
| DTO 검증 실패 | `MethodArgumentNotValidException` | `VALIDATION_ERROR` | 400 | 요청 필드가 제약 조건을 만족하지 않음 |

### 왜 Controller가 아니라 Service에서 검사할까?

Controller는 HTTP 요청을 DTO로 받고 서비스 메서드를 호출하는 데 집중한다. 반면 소유자 검증, 주문 상태 검증, 중복 상품 검증처럼 **다른 호출 경로에서도 반드시 지켜야 하는 규칙**은 서비스에 둔다.

```java
// PaymentService의 예: 주문 소유자가 아니면 서비스가 예외를 던진다.
if (!order.getUser().getId().equals(userId)) {
    throw new OrderAccessDeniedException();
}
```

이렇게 하면 나중에 같은 서비스를 배치 작업이나 다른 API에서 사용해도 권한/상태 규칙을 빼먹지 않는다.

## 3. 입력값 검증 예외: `@Valid`

Controller의 요청 DTO 앞에는 보통 `@Valid`가 있다.

```java
public ResponseEntity<PaymentConfirmResponse> confirm(
        @Valid @RequestBody PaymentConfirmRequest request
) { ... }
```

`PaymentConfirmRequest`의 `@NotBlank`, `@NotNull`, `@PositiveOrZero`, `@Size` 같은 제약을 만족하지 않으면 Controller 본문은 실행되지 않는다. Spring이 `MethodArgumentNotValidException`을 던지고, Advice의 `handleInvalidArgument()`가 모든 필드 오류 메시지를 `details` 배열에 모아 `400`으로 응답한다.

예시:

```json
{
  "message": "입력값이 올바르지 않습니다.",
  "details": [
    "토스 결제 키는 필수입니다.",
    "결제 금액은 0 이상이어야 합니다."
  ]
}
```

`@Valid`는 **형식 검증**만 한다. 예를 들어 `amount`가 0 이상인지는 검증하지만, 해당 금액이 주문 금액과 같은지는 `PaymentService`의 업무 검증이 담당한다.

## 4. JWT 인증 오류는 별도 경로

JWT 오류는 Controller에 도달하기 전에 발생할 수 있으므로 `GlobalExceptionHandler`가 아니라 Spring Security의 `JwtAuthenticationEntryPoint`가 처리한다.

구현 위치:

- `global/security/jwt/JwtAuthenticationFilter.java`
- `global/security/JwtAuthenticationEntryPoint.java`

흐름은 다음과 같다.

1. `JwtAuthenticationFilter`가 `Authorization: Bearer ...` 헤더를 읽는다.
2. 토큰이 만료됐거나 잘못됐다면 요청 속성에 `EXPIRED_TOKEN` 또는 `INVALID_TOKEN`을 기록한다. 이 단계에서는 바로 응답하지 않고 필터 체인을 계속 진행한다.
3. 보호된 API는 인증 정보가 없으므로 Spring Security가 `JwtAuthenticationEntryPoint`를 호출한다.
4. EntryPoint는 `401`과 `ErrorResponse` JSON을 직접 쓴다.

만료된 토큰의 응답 예시:

```json
{
  "code": "EXPIRED_TOKEN",
  "message": "로그아웃 되었습니다. 재로그인 하세요",
  "details": []
}
```

프론트는 이 `code`를 보고 토큰 재발급 또는 재로그인 화면 이동을 결정할 수 있다. 토큰이 아예 없는 요청도 `UNAUTHORIZED` 코드를 받는다.

## 5. 결제 오류 처리 예시

결제 확인 API는 두 부류의 실패를 구분한다.

1. **클라이언트/업무 오류**: 주문 금액 불일치, 이미 승인된 시도, 잘못된 주문 상태 등. `InvalidPaymentStateException`이 발생하며 `409`를 반환한다.
2. **외부 결제사 오류**: 토스 API 거절, 통신 오류 등. `TossPaymentClient`가 실패 결과를 반환하고 `PaymentAttempt`를 `FAILED`로 기록한다. Controller는 `502 Bad Gateway`를 반환하며, `PaymentConfirmResponse.code`에 `COMMUNICATION_ERROR`, `TOSS_400` 같은 실패 코드를 넣는다.

두 번째 경우는 외부 결제 API를 호출한 결과이므로 단순히 예외를 던져 트랜잭션을 되돌리지 않는다. 실패 이력을 남겨야 사용자가 재시도하거나 운영자가 원인을 추적할 수 있기 때문이다.

## 6. 새 예외를 추가하는 방법

예를 들어 “주문이 만료됨”을 별도 오류로 표현하고 싶다면 다음 세 단계를 따른다.

1. `ErrorCode`에 `ORDER_EXPIRED(HttpStatus.CONFLICT)`를 추가한다.
2. `global/exception`에 `OrderExpiredException extends BusinessException`을 만들고, 생성자에서 `ErrorCode.ORDER_EXPIRED`를 전달한다.
3. 서비스의 주문 상태 검사에서 해당 예외를 던진다. 별도 핸들러는 필요 없다.

```java
@ExceptionHandler(OrderExpiredException.class)
public class OrderExpiredException extends BusinessException {
    public OrderExpiredException() {
        super(ErrorCode.ORDER_EXPIRED, "만료된 주문입니다.");
    }
}
```

HTTP 상태는 프론트가 재시도 가능 여부를 판단하는 계약이므로, 같은 의미의 오류에는 같은 상태 코드를 유지하는 것이 좋다.

## 7. 현재 구조에서 알아둘 점

- 처리되지 않은 예외(예: `NullPointerException`, DB 연결 오류)는 최상위 핸들러에서 로그로 남기고 `INTERNAL_SERVER_ERROR` 코드의 일반화된 `500` 응답으로 처리한다. 내부 예외 메시지와 스택 트레이스는 프론트에 노출하지 않는다.
- 결제사 통신 실패는 의도적으로 `FAILED` 이력을 남기므로, 일반적인 예외 롤백과 동작이 다르다.
- 오류 메시지는 현재 한국어 문자열 중심이다. 프론트는 메시지에 의존해 분기하지 말고 안정적인 `code` 필드로 분기해야 한다.
