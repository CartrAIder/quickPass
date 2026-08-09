package com.mart.quickpass.user.controller;

import com.mart.quickpass.user.dto.SignUpRequest;
import com.mart.quickpass.user.dto.WithdrawUserRequest;
import com.mart.quickpass.user.service.UserService;
import com.mart.quickpass.user.service.UserWithdrawalService;
import com.mart.quickpass.global.config.AuthCookieProperties;
import com.mart.quickpass.global.security.jwt.JwtConstants;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;
    private final AuthCookieProperties authCookieProperties;

    // 회원가입 컨트롤러
    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest request) {
        userService.signUp(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 회원퇄퇴 컨트롤러
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WithdrawUserRequest request,
            HttpServletResponse response) {
        userWithdrawalService.withdraw(userId, request);
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie
                .from(JwtConstants.REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build().toString());
        return ResponseEntity.noContent().build();
    }
}
