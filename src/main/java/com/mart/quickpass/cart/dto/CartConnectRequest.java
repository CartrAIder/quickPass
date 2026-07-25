package com.mart.quickpass.cart.dto;

import jakarta.validation.constraints.NotBlank;

public record CartConnectRequest(

        @NotBlank(message = "카트 QR 코드의 인식에 실패했습니다.")
        String qrCode // 연결할 카트의 QR 코드
) {
}
