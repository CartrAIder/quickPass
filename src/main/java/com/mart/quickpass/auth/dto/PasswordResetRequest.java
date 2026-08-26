package com.mart.quickpass.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 인증 완료 후 새 비밀번호로 변경하기 위한 요청 DTO
public record PasswordResetRequest(
        @NotBlank(message = "비밀번호 재설정 토큰은 필수입니다.")
        String resetToken, // 비밀번호 재설정 토큰

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=]).{8,20}$",
                message = "새 비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다."
        )
        String newPassword // 새 비밀번호
) {
}
