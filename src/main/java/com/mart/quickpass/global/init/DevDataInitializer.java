package com.mart.quickpass.global.init;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.gate.service.GateTokenService;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderItem;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderItemRepository;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
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

import java.time.LocalDateTime;
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
    private static final String DEMO_ORDER_ID = "DEV-ORDER-001";
    private static final String DEMO_PAYMENT_ATTEMPT_ID = "DEV-PAYMENT-ATTEMPT-001";
    private static final String DEMO_PAYMENT_KEY = "DEV-PAYMENT-KEY-001";
    private static final String CANCELED_DEMO_ORDER_ID = "DEV-ORDER-002";
    private static final String EXPIRED_DEMO_ORDER_ID = "DEV-ORDER-003";
    private static final int DEMO_PRODUCT_PRICE = 1_000;

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final GateTokenService gateTokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser(TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_USER_NAME, UserRole.USER);
        seedUser(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NAME, UserRole.ADMIN);
        seedCart(CART_001_QR, CartStatus.WAITING);
        demoProducts().forEach(this::seedProduct);

        seedOrder(
                DEMO_ORDER_ID,
                OrderStatus.PAID,
                "8801234567890", 2,
                "8801234567891", 1
        );
        seedApprovedPaymentAndGateToken(DEMO_ORDER_ID);
        seedOrder(
                CANCELED_DEMO_ORDER_ID,
                OrderStatus.CANCELED,
                "8801234567892", 1,
                "8801234567893", 3
        );
        seedOrder(
                EXPIRED_DEMO_ORDER_ID,
                OrderStatus.EXPIRED,
                "8801234567908", 2,
                "8801234567929", 2
        );
    }

    /**
     * 바코드 스캔 및 장바구니 화면 테스트용 추가 상품 50개.
     * 기존 3개 상품과 바코드가 겹치지 않도록 8801234567893부터 사용한다.
     */
    private List<DemoProduct> demoProducts() {
        return List.of(
                new DemoProduct("0000289908820", "알로에", ProductCategory.BEVERAGE),
                new DemoProduct("0000196114796", "사과", ProductCategory.FOOD),
                new DemoProduct("0000008526731", "가방", ProductCategory.FASHION_ACCESSORIES),
                new DemoProduct("0000545710723", "밴드", ProductCategory.HOUSEHOLD),
                new DemoProduct("0000554237457", "건전지(AA)", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000238748446", "시계", ProductCategory.HOUSEHOLD),
                new DemoProduct("0000426336509", "콜라", ProductCategory.BEVERAGE),
                new DemoProduct("0000443389960", "큐브", ProductCategory.TOYS_HOBBIES),
                new DemoProduct("0000034106921", "공학용계산기", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000409283042", "칼", ProductCategory.TOYS_HOBBIES),
                new DemoProduct("0000562764181", "뒤집개", ProductCategory.KITCHENWARE),
                new DemoProduct("0000025580198", "게임패드", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000579817641", "악력기", ProductCategory.SPORTS_LEISURE),
                new DemoProduct("0000511603806", "핸드크림", ProductCategory.BEAUTY),
                new DemoProduct("0000460443423", "키보드", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000093794039", "립밤", ProductCategory.BEAUTY),
                new DemoProduct("0000630978021", "모니터", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000119374221", "마우스", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000349595939", "접시", ProductCategory.KITCHENWARE),
                new DemoProduct("0000588344374", "보조배터리", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000605397833", "각티슈", ProductCategory.HOUSEHOLD),
                new DemoProduct("0000392229584", "치약", ProductCategory.HOUSEHOLD),
                new DemoProduct("0000315489019", "우산", ProductCategory.HOUSEHOLD),
                new DemoProduct("0000596871107", "USB메모리", ProductCategory.DIGITAL_ELECTRONICS),
                new DemoProduct("0000358122669", "지갑", ProductCategory.FASHION_ACCESSORIES),
                new DemoProduct("0000051160388", "김", ProductCategory.FOOD),
                new DemoProduct("0000068213848", "노세범", ProductCategory.BEAUTY),
                new DemoProduct("0000110847496", "마스크팩", ProductCategory.BEAUTY),
                new DemoProduct("0000153481145", "배홍동칼빔면", ProductCategory.FOOD),
                new DemoProduct("0000213168252", "세럼", ProductCategory.BEAUTY),
                new DemoProduct("0000230221718", "수분크림", ProductCategory.BEAUTY),
                new DemoProduct("0000255801902", "썬스틱", ProductCategory.BEAUTY),
                new DemoProduct("0000306962286", "오뚜기작은밥", ProductCategory.FOOD),
                new DemoProduct("0000324015742", "운동화", ProductCategory.FASHION_ACCESSORIES),
                new DemoProduct("0000434863233", "쿠션", ProductCategory.BEAUTY),
                new DemoProduct("0000486023616", "통조림닭가슴살", ProductCategory.FOOD),
                new DemoProduct("0000537183993", "휴지", ProductCategory.HOUSEHOLD)
        );
    }

    private void seedApprovedPaymentAndGateToken(String orderId) {
        Order order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            log.debug("[DevData] 상품이 없어 생성되지 않은 주문 '{}'의 결제 시드를 생략", orderId);
            return;
        }

        if (paymentAttemptRepository.findByPaymentAttemptId(DEMO_PAYMENT_ATTEMPT_ID).isEmpty()) {
            LocalDateTime approvedAt = LocalDateTime.now();
            PaymentAttempt paymentAttempt = PaymentAttempt.builder()
                    .paymentAttemptId(DEMO_PAYMENT_ATTEMPT_ID)
                    .order(order)
                    .paymentKey(DEMO_PAYMENT_KEY)
                    .provider("TOSS_PAYMENTS")
                    .method("CARD")
                    .requestedAmount(order.getTotalAmount())
                    .approvedAmount(order.getTotalAmount())
                    .status(PaymentStatus.APPROVED)
                    .providerStatus("DONE")
                    .requestedAt(approvedAt)
                    .approvedAt(approvedAt)
                    .build();
            paymentAttemptRepository.save(paymentAttempt);
            log.debug("[DevData] 승인 결제 시드 완료 - paymentAttemptId={}, orderId={}, amount={}",
                    DEMO_PAYMENT_ATTEMPT_ID, orderId, order.getTotalAmount());
        } else {
            log.debug("[DevData] 승인 결제 '{}' 이미 존재 - 시드 생략", DEMO_PAYMENT_ATTEMPT_ID);
        }

        String gateToken = gateTokenService.issue(order.getId());
        log.warn("[개발용 게이트 토큰] 주문번호={} | 토큰={}", orderId, gateToken);
    }

    private void seedCart(String qrCode, CartStatus status) {
        if (cartRepository.existsByQrCode(qrCode)) {
            log.debug("[DevData] 카트 '{}' 이미 존재 - 시드 생략", qrCode);
            return;
        }
        Cart cart = Cart.builder()
                .qrCode(qrCode)
                .status(status)
                .build();
        cartRepository.save(cart);
        log.debug("[DevData] 가상 카트 시드 완료 - qrCode={}, status={}", qrCode, status);
    }

    private void seedProduct(DemoProduct demoProduct) {
        if (productRepository.existsByBarcode(demoProduct.barcode())) {
            log.debug("[DevData] 상품 '{}' 이미 존재 - 시드 생략", demoProduct.name());
            return;
        }
        Product product = Product.builder()
                .barcode(demoProduct.barcode())
                .name(demoProduct.name())
                .price(DEMO_PRODUCT_PRICE)
                .category(demoProduct.category())
                .status(ProductStatus.ON_SALE)
                .build();
        productRepository.save(product);
        log.debug("[DevData] 상품 시드 완료 - barcode={}, name={}",
                demoProduct.barcode(), demoProduct.name());
    }

    private void seedOrder(
            String orderId,
            OrderStatus status,
            String firstProductBarcode,
            int firstProductQuantity,
            String secondProductBarcode,
            int secondProductQuantity
    ) {
        if (orderRepository.findByOrderId(orderId).isPresent()) {
            log.debug("[DevData] 주문 '{}' 이미 존재 - 시드 생략", orderId);
            return;
        }

        User user = userRepository.findByEmail(TEST_USER_EMAIL)
                .orElseThrow(() -> new IllegalStateException("초기 주문 사용자를 찾을 수 없습니다."));
        Cart cart = cartRepository.findByQrCode(CART_001_QR)
                .orElseThrow(() -> new IllegalStateException("초기 주문 카트를 찾을 수 없습니다."));
        Product firstProduct = productRepository.findByBarcode(firstProductBarcode).orElse(null);
        Product secondProduct = productRepository.findByBarcode(secondProductBarcode).orElse(null);
        if (firstProduct == null || secondProduct == null) {
            log.debug("[DevData] 주문 '{}'에 필요한 상품이 없어 주문 시드를 생략", orderId);
            return;
        }
        long firstLineAmount = (long) firstProduct.getPrice() * firstProductQuantity;
        long secondLineAmount = (long) secondProduct.getPrice() * secondProductQuantity;

        Order order = orderRepository.save(Order.builder()
                .orderId(orderId)
                .user(user)
                .cart(cart)
                .orderName(firstProduct.getName() + " 외 1건")
                .totalAmount(firstLineAmount + secondLineAmount)
                .status(status)
                .paidAt(status == OrderStatus.PAID || status == OrderStatus.CANCELED
                        ? LocalDateTime.now()
                        : null)
                .build());

        orderItemRepository.saveAll(List.of(
                createOrderItem(order, firstProduct, firstProductQuantity, firstLineAmount),
                createOrderItem(order, secondProduct, secondProductQuantity, secondLineAmount)
        ));
        log.debug("[DevData] 주문 시드 완료 - orderId={}, status={}, totalAmount={}",
                orderId, status, order.getTotalAmount());
    }

    private OrderItem createOrderItem(Order order, Product product, int quantity, long lineAmount) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .unitPrice(product.getPrice().longValue())
                .quantity(quantity)
                .lineAmount(lineAmount)
                .build();
    }

    private void seedUser(String email, String rawPassword, String name, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            log.debug("[DevData] 사용자 '{}' 이미 존재 - 시드 생략", email);
            return;
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(role)
                .build();
        userRepository.save(user);
        log.debug("[DevData] 테스트 사용자 시드 완료 - email={}, name={}", email, name);
    }

    private record DemoProduct(String barcode, String name, ProductCategory category) {
    }
}
