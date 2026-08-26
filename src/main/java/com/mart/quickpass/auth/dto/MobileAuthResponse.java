package com.mart.quickpass.auth.dto;

// 모바일 인증 성공 후 토큰과 사용자 정보를 전달하는 응답 DTO
public record MobileAuthResponse(
        String accessToken, // 액세스 토큰
        String refreshToken, // 리프레시 토큰
        long accessTokenExpiresIn, // 액세스 토큰 만료시간(초)
        long refreshTokenExpiresIn, // 리프레시 토큰 만료시간(초)
        String tokenType, // Authorization 헤더에 사용하는 토큰 유형
        String name // 사용자 이름
) {
}
