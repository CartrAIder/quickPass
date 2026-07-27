package com.mart.quickpass.auth.controller;

import com.mart.quickpass.auth.dto.AuthTokens;
import com.mart.quickpass.auth.dto.LoginRequest;
import com.mart.quickpass.auth.dto.MobileAuthResponse;
import com.mart.quickpass.auth.dto.RefreshTokenRequest;
import com.mart.quickpass.auth.service.AuthService;
import com.mart.quickpass.global.config.JwtProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React Native 등 쿠키를 사용하지 않는 네이티브 클라이언트용 인증 API.
 * Refresh Token은 응답/요청 body로만 전달하며, 클라이언트는 OS 보안 저장소에 보관해야 한다.
 */
@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<MobileAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(toResponse(authService.login(request)));
    }

    @PostMapping("/reissue")
    public ResponseEntity<MobileAuthResponse> reissue(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(toResponse(authService.reissue(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    private MobileAuthResponse toResponse(AuthTokens tokens) {
        return new MobileAuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                jwtProperties.accessTokenValidity() / 1000,
                jwtProperties.refreshTokenValidity() / 1000,
                "Bearer",
                tokens.name()
        );
    }
}
