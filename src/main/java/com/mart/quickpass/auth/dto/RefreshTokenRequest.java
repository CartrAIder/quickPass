package com.mart.quickpass.auth.dto;

import jakarta.validation.constraints.NotBlank;

// 모바일 환경에서 리프레시 토큰을 전달하는 요청 DTO
public record RefreshTokenRequest(
        @NotBlank String refreshToken // 액세스 토큰 재발급에 사용할 리프레시 토큰
) {
}
