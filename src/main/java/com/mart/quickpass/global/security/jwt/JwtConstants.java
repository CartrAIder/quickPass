package com.mart.quickpass.global.security.jwt;

public final class JwtConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    // 인증 실패 사유를 EntryPoint에 전달하기 위한 요청 속성 키 및 코드
    public static final String TOKEN_ERROR_ATTRIBUTE = "tokenErrorCode";
    public static final String ERROR_EXPIRED_TOKEN = "EXPIRED_TOKEN";
    public static final String ERROR_INVALID_TOKEN = "INVALID_TOKEN";

    private JwtConstants() {
        // 헤더에서 토큰 및 userId 추천
    }
}
