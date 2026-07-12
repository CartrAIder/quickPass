package com.mart.quickpass.cart.dto;

import jakarta.validation.constraints.NotBlank;

public record CartConnectRequest(

        @NotBlank(message = "카트 QR 코드는 필수입니다.")
        String qrCode
) {
}
