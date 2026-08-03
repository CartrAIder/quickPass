package com.mart.quickpass.cart.event;


// 장바구니 상태 변경의 종류
public enum CartChangeType {

    // 카트 qr 스캔 -> 초기 세션 생성
    INITIALIZED("cart-init"),
    RESUMED("cart-resumed"),

    // 바코드 스캔, 유저 조작으로 인해 장바구니 내용 변경
    UPDATED("cart-updated"),

    // 카트 반납(쇼핑 취소, 결제 완료)
    CLOSED("cart-closed");

    private final String eventName;

    CartChangeType(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
