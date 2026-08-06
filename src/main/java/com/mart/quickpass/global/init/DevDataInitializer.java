package com.mart.quickpass.global.init;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
// 개발용 초기 데이터
@Slf4j
// TODO: 배포 전 @Profile("local") 복구 (지금은 프로필 무관하게 시드되도록 임시로 꺼둠)
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private static final String CART_001_QR = "cart_001";
    private static final String TEST_USER_EMAIL = "test@test.com";
    private static final String TEST_USER_PASSWORD = "1234test@";
    private static final String TEST_USER_NAME = "김춘식";
    private static final String ADMIN_EMAIL = "admin@cartraider.com";
    private static final String ADMIN_PASSWORD = "admin1234@";
    private static final String ADMIN_NAME = "관리자";

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME, UserRole.USER);
        seedUser(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NAME, UserRole.ADMIN);
        seedCart(CART_001_QR, CartStatus.AVAILABLE);

        // 장바구니 스캔 데모용 상품 (바코드는 시뮬레이터/문서 예시와 맞춤)
        seedProduct("8801234567890", "라면", 10, "식품");
        seedProduct("8801234567891", "샴푸", 9800, "생활용품");
        seedProduct("8801234567892", "삼겹살", 15000, "정육");

        demoProducts().forEach(product ->
                seedProduct(product.barcode(), product.name(), product.price(), product.category()));
    }

    /**
     * 바코드 스캔 및 장바구니 화면 테스트용 추가 상품 50개.
     * 기존 3개 상품과 바코드가 겹치지 않도록 8801234567893부터 사용한다.
     */
    private List<DemoProduct> demoProducts() {
        return List.of(
                new DemoProduct("8801234567893", "신라면", 1200, "식품"),
                new DemoProduct("8801234567894", "짜파게티", 1300, "식품"),
                new DemoProduct("8801234567895", "진라면 매운맛", 1000, "식품"),
                new DemoProduct("8801234567896", "컵라면", 1500, "식품"),
                new DemoProduct("8801234567897", "즉석밥", 1800, "식품"),
                new DemoProduct("8801234567898", "참치캔", 2500, "식품"),
                new DemoProduct("8801234567899", "스팸 클래식", 5200, "식품"),
                new DemoProduct("8801234567900", "김치", 7900, "식품"),
                new DemoProduct("8801234567901", "맛김", 3200, "식품"),
                new DemoProduct("8801234567902", "올리브유", 12900, "식품"),
                new DemoProduct("8801234567903", "우유 1L", 2900, "유제품"),
                new DemoProduct("8801234567904", "딸기우유", 1700, "유제품"),
                new DemoProduct("8801234567905", "요구르트", 2600, "유제품"),
                new DemoProduct("8801234567906", "체다치즈", 4800, "유제품"),
                new DemoProduct("8801234567907", "계란 10구", 6500, "유제품"),
                new DemoProduct("8801234567908", "생수 500ml", 800, "음료"),
                new DemoProduct("8801234567909", "생수 2L", 1500, "음료"),
                new DemoProduct("8801234567910", "콜라 500ml", 2100, "음료"),
                new DemoProduct("8801234567911", "사이다 500ml", 2100, "음료"),
                new DemoProduct("8801234567912", "오렌지주스", 3500, "음료"),
                new DemoProduct("8801234567913", "아메리카노 캔", 1800, "음료"),
                new DemoProduct("8801234567914", "이온음료", 2000, "음료"),
                new DemoProduct("8801234567915", "닭가슴살", 6900, "정육"),
                new DemoProduct("8801234567916", "소고기 불고기", 18900, "정육"),
                new DemoProduct("8801234567917", "돼지 목살", 13900, "정육"),
                new DemoProduct("8801234567918", "훈제오리", 10900, "정육"),
                new DemoProduct("8801234567919", "두부", 1800, "냉장식품"),
                new DemoProduct("8801234567920", "콩나물", 1500, "채소"),
                new DemoProduct("8801234567921", "양파 1kg", 3400, "채소"),
                new DemoProduct("8801234567922", "감자 1kg", 3900, "채소"),
                new DemoProduct("8801234567923", "바나나", 4900, "과일"),
                new DemoProduct("8801234567924", "사과 4입", 7900, "과일"),
                new DemoProduct("8801234567925", "방울토마토", 5900, "과일"),
                new DemoProduct("8801234567926", "냉동만두", 8500, "냉동식품"),
                new DemoProduct("8801234567927", "냉동피자", 9900, "냉동식품"),
                new DemoProduct("8801234567928", "아이스크림", 3500, "냉동식품"),
                new DemoProduct("8801234567929", "감자칩", 2500, "과자"),
                new DemoProduct("8801234567930", "초코파이", 4800, "과자"),
                new DemoProduct("8801234567931", "쿠키", 3300, "과자"),
                new DemoProduct("8801234567932", "물티슈", 2900, "생활용품"),
                new DemoProduct("8801234567933", "화장지 12롤", 8900, "생활용품"),
                new DemoProduct("8801234567934", "주방세제", 4900, "생활용품"),
                new DemoProduct("8801234567935", "세탁세제", 11900, "생활용품"),
                new DemoProduct("8801234567936", "칫솔 4입", 5500, "생활용품"),
                new DemoProduct("8801234567937", "치약", 3200, "생활용품"),
                new DemoProduct("8801234567938", "마스크 10매", 3900, "생활용품"),
                new DemoProduct("8801234567939", "건전지 AA 4입", 4500, "생활용품"),
                new DemoProduct("8801234567940", "고양이 사료", 15900, "반려동물"),
                new DemoProduct("8801234567941", "강아지 간식", 6900, "반려동물"),
                new DemoProduct("8801234567942", "종량제 봉투", 2200, "생활용품")
        );
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

    private void seedUser(String email, String rawPassword, String name, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            log.info("[DevData] 사용자 '{}' 이미 존재 - 시드 생략", email);
            return;
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(role)
                .build();
        userRepository.save(user);
        log.info("[DevData] 테스트 사용자 시드 완료 - email={}, name={}", email, name);
    }

    private record DemoProduct(String barcode, String name, int price, String category) {
    }
}
