package com.mart.quickpass.auth.controller;

import com.mart.quickpass.auth.dto.AuthTokens;
import com.mart.quickpass.auth.dto.LoginRequest;
import com.mart.quickpass.auth.service.AuthService;
import com.mart.quickpass.global.config.JwtProperties;
import com.mart.quickpass.global.security.jwt.JwtConstants;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    // 로그인 컨트롤러
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthTokens tokens = authService.login(request);
        setTokenResponse(response, tokens);

        return ResponseEntity.ok().build();
    }

    // 토큰 재발급 컨트롤러 - 쿠키의 리프레시 토큰으로 Access/Refresh 토큰 모두 재발급
    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(
            @CookieValue(JwtConstants.REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
            HttpServletResponse response) {
        AuthTokens tokens = authService.reissue(refreshToken);
        setTokenResponse(response, tokens);

        return ResponseEntity.ok().build();
    }

    // 로그아웃 컨트롤러 - 저장된 리프레시 토큰 삭제 및 쿠키 만료
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(JwtConstants.REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie().toString());

        return ResponseEntity.ok().build();
    }

    // Access Token은 헤더, Refresh Token은 쿠키로 응답에 실어준다
    private void setTokenResponse(HttpServletResponse response, AuthTokens tokens) {
        response.setHeader(HttpHeaders.AUTHORIZATION, JwtConstants.TOKEN_PREFIX + tokens.accessToken());
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(tokens.refreshToken()).toString());
    }

    // 리프레시 토큰 보안 설정
    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(false) // 배포 시 true로 변경 필요
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenValidity()))
                .build();
    }

    // 로그아웃 시 리프레시 토큰 쿠키를 즉시 만료시킨다
    private ResponseCookie expireRefreshTokenCookie() {
        return ResponseCookie.from(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(false) // 배포 시 true로 변경 필요
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }
}
