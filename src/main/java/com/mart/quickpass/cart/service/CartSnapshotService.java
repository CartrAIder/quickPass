package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.dto.CartSnapshotResponse;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartSnapshotService {

    private final CartItemsRepository cartItemsRepository;

    // 특정 장바구니의 전체 데이터를 CartSnapshotResponse DTO로 조립하여 반환
    public CartSnapshotResponse snapshot(String qrCode, long version) {
        Map<String, CartItem> items = cartItemsRepository.findAllItems(qrCode);
        return CartSnapshotResponse.of(qrCode, version, items);
    }
}
