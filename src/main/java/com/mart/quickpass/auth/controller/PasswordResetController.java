package com.mart.quickpass.auth.controller;

import com.mart.quickpass.auth.dto.PasswordResetConfirmRequest;
import com.mart.quickpass.auth.dto.PasswordResetMessageResponse;
import com.mart.quickpass.auth.dto.PasswordResetRequest;
import com.mart.quickpass.auth.dto.PasswordResetSendRequest;
import com.mart.quickpass.auth.dto.PasswordResetTokenResponse;
import com.mart.quickpass.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String SEND_RESPONSE_MESSAGE =
            "가입된 이메일이라면 인증번호를 전송했습니다.";

    private final PasswordResetService passwordResetService;

    // 인증번호 발송 컨트롤러
    @PostMapping("/code")
    public ResponseEntity<PasswordResetMessageResponse> sendCode(
            @Valid @RequestBody PasswordResetSendRequest request
    ) {
        passwordResetService.sendCode(request.email());
        return ResponseEntity.ok(new PasswordResetMessageResponse(SEND_RESPONSE_MESSAGE));
    }

    // 인증번호 확인 컨트롤러
    @PostMapping("/confirm")
    public ResponseEntity<PasswordResetTokenResponse> confirmCode(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        return ResponseEntity.ok(passwordResetService.confirmCode(request));
    }

    // 비밀번호 재설정 컨트롤러
    @PostMapping
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
