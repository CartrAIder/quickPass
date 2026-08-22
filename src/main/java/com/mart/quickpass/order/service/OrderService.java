package com.mart.quickpass.order.service;

import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.global.exception.CartSessionNotFoundException;
import com.mart.quickpass.global.exception.DuplicateOrderProductException;
import com.mart.quickpass.global.exception.InvalidProductPriceException;
import com.mart.quickpass.global.exception.InvalidPaymentStateException;
import com.mart.quickpass.global.exception.OrderAccessDeniedException;
import com.mart.quickpass.global.exception.OrderNotFoundException;
import com.mart.quickpass.global.exception.ProductNotFoundException;
import com.mart.quickpass.global.exception.ProductNotOnSaleException;
import com.mart.quickpass.global.exception.UserNotFoundException;
import com.mart.quickpass.order.dto.OrderCreateItemRequest;
import com.mart.quickpass.order.dto.OrderCreateRequest;
import com.mart.quickpass.order.dto.OrderCreateResponse;
import com.mart.quickpass.order.dto.OrderCreateResult;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderItem;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderItemRepository;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int ORDER_NAME_MAX_LENGTH = 100;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartSessionRepository cartSessionRepository;
    private final CartRepository cartRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;

    // 주문 생성 메서드
    @Transactional
    public OrderCreateResult create(Long userId, OrderCreateRequest request) {
        String qrCode = cartSessionRepository.findQrCodeByUserId(userId)
                .orElseThrow(() -> new CartSessionNotFoundException("current"));
        CartSession session = cartSessionRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartSessionNotFoundException(qrCode));
        if (!session.userId().equals(userId)) {
            throw new CartSessionNotFoundException(qrCode);
        }
        // 사용자 행을 잠가 같은 사용자의 주문 생성 요청을 직렬화한다.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Cart cart = cartRepository.findByQrCodeForUpdate(qrCode)
                .orElseThrow(() -> new CartSessionNotFoundException(qrCode));
        // 카트 잠금을 기다리는 동안 세션이 반납될 수 있으므로 잠금 획득 후 소유권을 재검증한다.
        CartSession lockedSession = cartSessionRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartSessionNotFoundException(qrCode));
        if (!lockedSession.userId().equals(userId)) {
            throw new CartSessionNotFoundException(qrCode);
        }

        return orderRepository.findByUserAndCartAndStatusForUpdate(
                        userId, cart.getId(), OrderStatus.PENDING_PAYMENT)
                .map(order -> OrderCreateResult.existing(OrderCreateResponse.from(order)))
                .orElseGet(() -> createNew(user, cart, request));
    }

    private OrderCreateResult createNew(User user, Cart cart, OrderCreateRequest request) {

        // 상품 중복 등록 확인
        validateDistinctProductIds(request.items());

        // 상품 id 목록 추출 및 조히
        List<Long> productIds = request.items().stream()
                .map(OrderCreateItemRequest::productId)
                .toList();
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 주문 상품 초안 생성
        List<OrderItemDraft> itemDrafts = request.items().stream()
                .map(item -> createItemDraft(item, productsById))
                .toList();

        // 주문 총액 계산
        long totalAmount = itemDrafts.stream()
                .mapToLong(OrderItemDraft::lineAmount)
                .reduce(0L, Math::addExact);
        if (totalAmount <= 0) {
            throw new InvalidProductPriceException(itemDrafts.getFirst().product().getId());
        }

        Order order = Order.builder()
                .orderId(generateExternalOrderId())
                .user(user)
                .cart(cart)
                .orderName(createOrderName(itemDrafts))
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = itemDrafts.stream()
                .map(draft -> OrderItem.builder()
                        .order(savedOrder)
                        .product(draft.product())
                        .productName(draft.product().getName())
                        .unitPrice(draft.unitPrice())
                        .quantity(draft.quantity())
                        .lineAmount(draft.lineAmount())
                        .build())
                .toList();
        orderItemRepository.saveAll(orderItems);

        return OrderCreateResult.created(OrderCreateResponse.from(savedOrder));
    }

    @Transactional
    public void abandon(Long userId, String orderId) {
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidPaymentStateException("포기할 수 없는 주문 상태입니다.");
        }
        if (paymentAttemptRepository.existsByOrder_IdAndStatusIn(
                order.getId(), List.of(PaymentStatus.APPROVED))) {
            throw new InvalidPaymentStateException("이미 승인된 결제가 있어 주문을 포기할 수 없습니다.");
        }

        paymentAttemptRepository.findAllByOrder_IdAndStatus(order.getId(), PaymentStatus.READY)
                .forEach(attempt -> attempt.cancel());
        order.expire();
    }

    // 상품 중복 검사 메서드
    private void validateDistinctProductIds(List<OrderCreateItemRequest> items) {
        Set<Long> productIds = new HashSet<>();
        for (OrderCreateItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new DuplicateOrderProductException(item.productId());
            }
        }
    }

    // 주문 초안 생성 메서드
    private OrderItemDraft createItemDraft(OrderCreateItemRequest item, Map<Long, Product> productsById) {
        Product product = productsById.get(item.productId());
        if (product == null) {
            throw new ProductNotFoundException(item.productId());
        }
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new ProductNotOnSaleException(item.productId());
        }

        long unitPrice = product.getPrice();
        if (unitPrice <= 0) {
            throw new InvalidProductPriceException(item.productId());
        }
        long lineAmount = Math.multiplyExact(unitPrice, item.quantity());
        return new OrderItemDraft(product, unitPrice, item.quantity(), lineAmount);
    }

    // 주문명 생성 메서드
    private String createOrderName(List<OrderItemDraft> itemDrafts) {
        String firstProductName = itemDrafts.getFirst().product().getName();
        if (itemDrafts.size() == 1) {
            return truncateOrderName(firstProductName, ORDER_NAME_MAX_LENGTH);
        }

        String suffix = " 외 " + (itemDrafts.size() - 1) + "건";
        return truncateOrderName(firstProductName, ORDER_NAME_MAX_LENGTH - suffix.length()) + suffix;
    }

    // 주문명 자르기 메서드
    private String truncateOrderName(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    // 외부 주문 ID 생성 메서드
    private String generateExternalOrderId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 내부용 객체
    private record OrderItemDraft(Product product, long unitPrice, int quantity, long lineAmount) {
    }
}
