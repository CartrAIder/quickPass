package com.mart.quickpass.global.init;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용 초기 데이터 시더.
 *
 * <p>{@code ddl-auto: create}로 매 기동 시 테이블이 비워지므로, 앱을 띄우면
 * 가상 카트/상품이 자동으로 채워진다. 중복 삽입은 {@code existsByQrCode}/{@code existsByBarcode}로 방지한다.
 *
 * <p><b>⚠️ 배포 전 필수 조치</b>: 현재는 프로필 제한 없이 <b>모든 환경에서 실행</b>된다.
 * 운영에 시드가 들어가지 않도록, 배포 전 {@code @Profile("local")}을 다시 붙일 것.
 */
@Slf4j
// TODO: 배포 전 @Profile("local") 복구 (지금은 프로필 무관하게 시드되도록 임시로 꺼둠)
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private static final String CART_001_QR = "cart_001";

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        seedCart(CART_001_QR, CartStatus.WAITING);

        // 장바구니 스캔 데모용 상품 (바코드는 시뮬레이터/문서 예시와 맞춤)
        seedProduct("8801234567890", "라면", 1200, "식품");
        seedProduct("8801234567891", "샴푸", 9800, "생활용품");
        seedProduct("8801234567892", "삼겹살", 15000, "정육");
    }

    private void seedCart(String qrCode, CartStatus status) {
        if (cartRepository.existsByQrCode(qrCode)) {
            log.info("[DevData] 카트 '{}' 이미 존재 - 시드 생략", qrCode);
            return;
        }
        Cart cart = Cart.builder()
                .qrCode(qrCode)
                .status(status)
                .build();
        cartRepository.save(cart);
        log.info("[DevData] 가상 카트 시드 완료 - qrCode={}, status={}", qrCode, status);
    }

    private void seedProduct(String barcode, String name, int price, String category) {
        if (productRepository.existsByBarcode(barcode)) {
            log.info("[DevData] 상품 '{}' 이미 존재 - 시드 생략", name);
            return;
        }
        Product product = Product.builder()
                .barcode(barcode)
                .name(name)
                .price(price)
                .category(category)
                .status(ProductStatus.ON_SALE)
                .build();
        productRepository.save(product);
        log.info("[DevData] 상품 시드 완료 - barcode={}, name={}, price={}", barcode, name, price);
    }
}
