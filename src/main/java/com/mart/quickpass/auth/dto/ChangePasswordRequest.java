package com.mart.quickpass.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 로그인한 사용자의 비밀번호 변경 요청 DTO
public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword, // 현재 비밀번호

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=]).{8,20}$",
                message = "새 비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다."
        )
        String newPassword // 새 비밀번호
) {
}
