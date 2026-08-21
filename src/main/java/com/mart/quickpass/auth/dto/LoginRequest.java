package com.mart.quickpass.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 이메일과 비밀번호를 전달하는 로그인 요청 DTO
public record LoginRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email, // 회원 이메일

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password // 회원 비밀번호
) {
}
