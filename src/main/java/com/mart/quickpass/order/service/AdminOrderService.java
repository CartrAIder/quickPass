package com.mart.quickpass.order.service;

import com.mart.quickpass.global.exception.OrderNotFoundException;
import com.mart.quickpass.order.dto.AdminOrderDetailResponse;
import com.mart.quickpass.order.dto.AdminOrderPageResponse;
import com.mart.quickpass.order.dto.AdminOrderSummaryResponse;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderItemRepository;
import com.mart.quickpass.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // 주문 목록 조회
    public AdminOrderPageResponse search(String keyword, OrderStatus status, int page, int size) {
        String normalizedKeyword = normalizeKeyword(keyword);

        // 페이지 크기 정규화
        int normalizedSize = Math.min(size, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(
                page,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        return AdminOrderPageResponse.from(orderRepository.searchForAdmin(normalizedKeyword, status, pageable)
                .map(AdminOrderSummaryResponse::from));
    }

    // 주문 상세 조회
    public AdminOrderDetailResponse findByOrderId(String orderId) {
        Order order = orderRepository.findByOrderIdWithUser(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return AdminOrderDetailResponse.of(
                order,
                orderItemRepository.findAllByOrderIdOrderByIdAsc(order.getId())
        );
    }

    // 검색어 공백 제거
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
