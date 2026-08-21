package com.mart.quickpass.auth.dto;

// 웹 로그인 성공 후 사용자 정보를 전달하는 응답 DTO
public record LoginResponse(
        String name // 사용자 이름
) {
}
