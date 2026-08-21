package com.mart.quickpass.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawUserRequest(
        // 비밀번호 변경 요청 dto
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword
) {
}
