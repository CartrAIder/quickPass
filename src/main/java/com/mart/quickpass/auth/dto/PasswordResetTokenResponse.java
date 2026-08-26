package com.mart.quickpass.auth.dto;

// 인증번호 확인 후 비밀번호 재설정 토큰을 전달하는 응답 DTO
public record PasswordResetTokenResponse(
        String resetToken, // 새 비밀번호 변경에 사용하는 일회성 토큰
        long expiresIn // 재설정 토큰 만료시간(초)
) {
}
