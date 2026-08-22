package com.mart.quickpass.order.service;

import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.order.dto.OrderCreateItemRequest;
import com.mart.quickpass.order.dto.OrderCreateRequest;
import com.mart.quickpass.order.dto.OrderCreateResult;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.repository.OrderItemRepository;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
class OrderCreationConcurrencyTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private CartSessionRepository cartSessionRepository;

    @Test
    void concurrentRequestsCreateExactlyOnePendingOrder() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        User user = userRepository.save(User.builder()
                .email("order-concurrency-" + suffix + "@example.com")
                .password("encoded")
                .name("동시성 사용자")
                .role(UserRole.USER)
                .build());
        Cart cart = cartRepository.save(Cart.builder()
                .qrCode("concurrency-cart-" + suffix)
                .status(CartStatus.IN_USE)
                .build());
        Product product = productRepository.save(Product.builder()
                .barcode("concurrency-product-" + suffix)
                .name("동시성 상품")
                .price(1000)
                .category(ProductCategory.SNACK)
                .status(ProductStatus.ON_SALE)
                .build());
        when(cartSessionRepository.findQrCodeByUserId(user.getId()))
                .thenReturn(Optional.of(cart.getQrCode()));
        when(cartSessionRepository.findByQrCode(cart.getQrCode()))
                .thenReturn(Optional.of(CartSession.start(user.getId())));

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(new OrderCreateItemRequest(product.getId(), 1)));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> createTogether(user.getId(), request, ready, start));
            var second = executor.submit(() -> createTogether(user.getId(), request, ready, start));
            ready.await();
            start.countDown();

            OrderCreateResult firstResult = first.get();
            OrderCreateResult secondResult = second.get();

            assertThat(firstResult.response().orderId())
                    .isEqualTo(secondResult.response().orderId());
            assertThat(List.of(firstResult.created(), secondResult.created()))
                    .containsExactlyInAnyOrder(true, false);

            Order order = orderRepository.findByOrderId(firstResult.response().orderId()).orElseThrow();
            orderItemRepository.deleteAll(orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId()));
            orderRepository.delete(order);
        } finally {
            productRepository.delete(product);
            cartRepository.delete(cart);
            userRepository.delete(user);
        }
    }

    private OrderCreateResult createTogether(
            Long userId,
            OrderCreateRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return orderService.create(userId, request);
    }
}
