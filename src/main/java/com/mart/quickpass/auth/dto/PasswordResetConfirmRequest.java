package com.mart.quickpass.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 이메일로 받은 비밀번호 재설정 인증번호 확인 요청 DTO
public record PasswordResetConfirmRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email, // 회원 이메일

        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
        String code // 이메일로 발송된 6자리 인증번호
) {
}
