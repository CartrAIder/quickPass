package com.mart.quickpass.auth.dto;

// 인증 처리 후 발급된 토큰과 사용자 정보를 전달하는 내부 DTO
public record AuthResult(
        String accessToken, // 액세스 토큰
        String refreshToken, // 리프레시 토큰
        String name // 사용자 이름
) {
}
