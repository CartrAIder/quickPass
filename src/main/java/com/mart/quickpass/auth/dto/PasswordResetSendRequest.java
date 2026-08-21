package com.mart.quickpass.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 비밀번호 재설정 인증번호 발송을 요청하는 DTO
public record PasswordResetSendRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email // 회원 이메일
) {
}
