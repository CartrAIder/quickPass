# 데모 상품 및 이미지 등록 실행 가이드

## 1. 목적

이 문서는 QuickPass 로컬 환경에서 다음 작업을 완료하는 방법을 설명한다.

1. 엑셀을 기반으로 작성된 데모 상품 37개를 MySQL에 생성한다.
2. 바코드 이름의 이미지 37개를 MinIO에 업로드한다.
3. 업로드한 이미지의 Object Key를 각 `Product.imageKey`에 저장한다.
4. 상품 조회 API에서 최종 `imageUrl`이 반환되는지 검증한다.

상품 생성과 이미지 등록은 서로 다른 과정이다. `DevDataInitializer`가 상품을 생성해도 이미지는 자동으로 업로드되지 않으므로 반드시 이미지 Importer를 별도로 실행해야 한다.

## 2. 현재 데이터와 매칭 규칙

원본 데이터를 프로젝트의 이미지 Importer 전용 폴더에 둔다.

```text
tools/product-image-importer/
├── import_images.py
└── images/
    ├── 0000008526731.png
    ├── 0000025580198.png
    └── ... 총 37개
```

엑셀에는 상품 37개가 있으며 `images/`의 이미지 37개와 바코드가 모두 일치한다. 이미지 파일은 로컬 초기 데이터이므로 Git에는 포함하지 않는다.

이미지 Importer는 파일명에서 확장자를 제외한 값을 바코드로 사용한다.

```text
이미지 파일: 0000289908820.png
                 ↓
상품 바코드: 0000289908820
                 ↓
MinIO Key: products/0000289908820/{uuid}.png
```

바코드는 숫자가 아니라 문자열이다. 앞의 `0`을 제거하면 DB의 상품과 매칭되지 않는다.

## 3. 사용되는 코드

### 상품 생성

`src/main/java/com/mart/quickpass/global/init/DevDataInitializer.java`가 애플리케이션 시작 시 상품 37개를 생성한다.

- 가격: 모두 `1000`
- 상태: 모두 `ON_SALE`
- 바코드와 상품명: `products.xlsx` 기준
- 카테고리: 엑셀 카테고리에 대응하는 `ProductCategory`
- 동일 바코드가 이미 있으면 생성하지 않음
- 초기 `imageKey`: `null`

### 이미지 등록

`tools/product-image-importer/import_images.py`가 이미지 디렉터리의 파일을 하나씩 읽고 다음 관리자 API를 호출한다.

```text
POST /api/admin/products/{barcode}/image?replace=false
```

Spring Boot의 처리 순서는 다음과 같다.

```text
바코드로 Product 조회
→ 이미지 형식과 크기 검사
→ MinIO에 이미지 업로드
→ Product.imageKey 저장
→ API 응답용 imageUrl 생성
```

Importer는 MinIO나 MySQL에 직접 접근하지 않는다. 따라서 서버의 인증, 이미지 검증 및 상품 비즈니스 규칙이 그대로 적용된다.

## 4. 실행 전 환경 변수 설정

`.env`에 관리자 계정과 Spring Boot 전용 계정이 모두 필요하다.

```properties
MINIO_ROOT_USER=MinIO 관리자 사용자명
MINIO_ROOT_PASSWORD=MinIO 관리자 비밀번호

MINIO_APP_USER=quickpass-api
MINIO_APP_PASSWORD=Spring 전용 MinIO 비밀번호

MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_URL=http://localhost:9000
```

`MINIO_ROOT_*`는 버킷과 사용자를 준비하는 초기화 컨테이너만 사용한다. Spring Boot는 권한이 제한된 `MINIO_APP_*` 계정을 사용한다.

## 5. MinIO 초기화

MinIO 서버를 실행한다.

```bash
docker compose up -d minio
```

MinIO 서버 실행만으로는 버킷이 생성되지 않는다. 초기화 컨테이너를 반드시 별도로 실행한다.

```bash
docker compose run --rm minio-init
```

성공하면 다음 메시지가 출력된다.

```text
MinIO product-images 초기화가 완료되었습니다.
```

이 과정에서 다음 항목이 준비된다.

- `product-images` 버킷
- 익명 이미지 조회 정책
- Spring Boot 전용 MinIO 사용자
- `product-images/products/*`에 대한 조회·업로드·삭제 권한

여러 번 실행해도 기존 버킷과 이미지는 삭제되지 않는다.

## 6. Spring Boot 실행과 상품 생성 확인

애플리케이션을 실행한다.

```bash
./gradlew bootRun
```

`DevDataInitializer`가 실행되면 상품 37개가 생성된다. 이 시점의 `imageKey`가 `null`인 것은 정상이다.

이미 실행 중인 애플리케이션에 초기화 코드를 새로 반영했다면 애플리케이션을 다시 시작해야 한다. 현재 로컬 설정은 Hibernate `ddl-auto: create`이므로 재시작 시 테이블 데이터가 다시 만들어지는 점에 주의한다.

## 7. 관리자 Access Token 준비

이미지 등록 API는 관리자 권한이 필요하다. 관리자 계정으로 다음 API에 로그인한다.

```text
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "관리자 이메일",
  "password": "관리자 비밀번호"
}
```

Access Token은 JSON 본문이 아니라 응답의 `Authorization` 헤더에 다음 형식으로 전달된다.

```text
Authorization: Bearer {access-token}
```

`Bearer ` 뒤의 토큰 값만 환경 변수에 넣는다.

```bash
export ADMIN_ACCESS_TOKEN='발급받은-access-token'
```

토큰과 비밀번호는 Git에 저장하지 않는다.

## 8. 이미지 37개 등록

프로젝트 루트에서 다음 명령을 실행한다.

```bash
python3 tools/product-image-importer/import_images.py \
  --base-url http://localhost:8080 \
  --token "$ADMIN_ACCESS_TOKEN"
```

이미지 디렉터리 인자를 생략하면 `tools/product-image-importer/images`를 자동으로 사용한다. 다른 폴더를 사용하려는 경우에만 첫 번째 인자로 경로를 전달한다.

최초 성공 시 마지막 요약은 다음 형태가 된다.

```text
SUMMARY uploaded=37 skipped=0 missing=0 failed=0
```

Importer는 파일 하나가 실패해도 다음 파일을 계속 처리한다.

## 9. 재실행과 이미지 교체

기본 실행은 이미 이미지가 연결된 상품을 건너뛴다.

```text
SUMMARY uploaded=0 skipped=37 missing=0 failed=0
```

기존 이미지를 새 파일로 교체하려면 `--replace`를 사용한다.

```bash
python3 tools/product-image-importer/import_images.py \
  --base-url http://localhost:8080 \
  --token "$ADMIN_ACCESS_TOKEN" \
  --replace
```

교체 시 새 이미지 업로드와 DB 반영이 성공한 후 이전 MinIO 객체를 삭제한다.

## 10. 결과 검증

### DB

`products.image_key`가 다음처럼 채워져 있어야 한다.

```text
products/0000289908820/{uuid}.png
```

확인 쿼리 예시:

```sql
SELECT barcode, name, image_key
FROM products
ORDER BY barcode;
```

전체 매칭 상태는 다음 쿼리로 확인한다.

```sql
SELECT
    COUNT(*) AS total_products,
    SUM(CASE WHEN image_key IS NOT NULL THEN 1 ELSE 0 END) AS products_with_image,
    SUM(CASE WHEN image_key IS NULL THEN 1 ELSE 0 END) AS products_without_image
FROM products;
```

정상 결과는 데모 상품 기준 `products_with_image = 37`, `products_without_image = 0`이다. 다른 상품이 DB에 함께 있다면 전체 개수는 달라질 수 있다.

### 상품 조회 API

상품 조회 응답에서는 내부 `imageKey`가 아니라 공개 `imageUrl`이 반환되어야 한다.

```text
GET http://localhost:8080/api/products
```

예상 형태:

```json
{
  "barcode": "0000289908820",
  "name": "알로에",
  "imageUrl": "http://localhost:9000/product-images/products/0000289908820/{uuid}.png"
}
```

브라우저에서 `imageUrl`을 열어 실제 이미지가 표시되는지 확인한다.

## 11. 오류별 확인 사항

### `imageKey`가 계속 `null`

- 이미지 Importer를 실행하지 않았거나 실행 중 실패한 상태다.
- Importer의 마지막 `SUMMARY`를 확인한다.
- Spring Boot가 변경된 코드로 재시작됐는지 확인한다.

### `missing` 발생

- 이미지 파일명 바코드와 DB의 `Product.barcode`가 일치하지 않는다.
- 바코드 앞의 `0`이 제거됐는지 확인한다.
- 데모 상품이 생성되기 전에 Importer를 실행했는지 확인한다.

### HTTP 401 또는 403

- 관리자 Access Token이 없거나 만료됐거나 일반 사용자 토큰이다.
- 관리자 계정으로 다시 로그인해 토큰을 갱신한다.

### HTTP 502 또는 `PRODUCT_IMAGE_UPLOAD_FAILED`

- MinIO가 실행 중인지 확인한다.
- `minio-init`을 실행했는지 확인한다.
- `.env`의 `MINIO_APP_USER`, `MINIO_APP_PASSWORD`가 초기화 시 사용한 값과 같은지 확인한다.

### `uploaded=0`, `skipped=37`

- 오류가 아니라 모든 상품에 이미지가 이미 연결된 상태다.
- 교체가 필요하면 `--replace`를 사용한다.

## 12. 전체 실행 순서 요약

```text
.env에 MinIO 관리자/앱 계정 설정
→ docker compose up -d minio
→ docker compose run --rm minio-init
→ Spring Boot 실행 또는 재시작
→ 상품 37개 생성 확인
→ 관리자 로그인 및 Access Token 준비
→ import_images.py 실행
→ DB image_key 확인
→ 상품 API imageUrl 확인
→ 실제 이미지 URL 조회 확인
```
