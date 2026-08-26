package com.mart.quickpass.auth.controller;

import com.mart.quickpass.auth.dto.AuthResult;
import com.mart.quickpass.auth.dto.ChangePasswordRequest;
import com.mart.quickpass.auth.dto.LoginRequest;
import com.mart.quickpass.auth.dto.MobileAuthResponse;
import com.mart.quickpass.auth.dto.RefreshTokenRequest;
import com.mart.quickpass.auth.service.AuthService;
import com.mart.quickpass.global.config.JwtProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    // 로그인 컨트롤러
    @PostMapping("/login")
    public ResponseEntity<MobileAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(toResponse(authService.login(request)));
    }

    // 재로그인 컨트롤러
    @PostMapping("/reissue")
    public ResponseEntity<MobileAuthResponse> reissue(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(toResponse(authService.reissue(request.refreshToken())));
    }

    // 로그아웃 컨트롤러
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    // 비밀번호 변경 컨트롤러
    @PostMapping("/password")
    public ResponseEntity<MobileAuthResponse> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(toResponse(authService.changePassword(userId, request)));
    }

    // AuthTokens -> MobileAuthResponse 변환 메서드
    private MobileAuthResponse toResponse(AuthResult tokens) {
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
