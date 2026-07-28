package com.mart.quickpass.email.controller;

import com.mart.quickpass.email.dto.EmailVerificationConfirmRequest;
import com.mart.quickpass.email.dto.EmailVerificationResponse;
import com.mart.quickpass.email.dto.EmailVerificationSendRequest;
import com.mart.quickpass.email.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    // 인증번호 전송 컨트롤러
    @PostMapping
    public ResponseEntity<EmailVerificationResponse> sendCode(
            @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        emailVerificationService.sendCode(request.email());
        return ResponseEntity.ok(new EmailVerificationResponse("인증번호를 이메일로 전송했습니다."));
    }

    // 인증번호 검증 컨트롤러
    @PostMapping("/confirm")
    public ResponseEntity<EmailVerificationResponse> confirmCode(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok(new EmailVerificationResponse("이메일 인증이 완료되었습니다."));
    }
}
