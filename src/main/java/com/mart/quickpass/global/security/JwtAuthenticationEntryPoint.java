package com.mart.quickpass.global.security;

import com.mart.quickpass.global.exception.ErrorResponse;
import com.mart.quickpass.global.security.jwt.JwtConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 인증되지 않은 요청이 보호된 리소스에 접근할 때 401(인증 실패) 응답을 일관된 JSON 형식으로 반환한다.
// 필터가 기록한 오류 코드(만료/유효하지 않음)를 읽어 프론트가 재발급 여부를 판단할 수 있게 한다.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // json 객체로 변환
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // HTTP 상태 코드 수동 지정(401)
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        // json 형태로 지정 및 UTF_8 사용
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 필터가 남긴 오류 코드에 따라 응답 본문을 구성 (없으면 단순 미인증)
        Object errorCode = request.getAttribute(JwtConstants.TOKEN_ERROR_ATTRIBUTE);
        ErrorResponse body = buildBody(errorCode == null ? null : errorCode.toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private ErrorResponse buildBody(String errorCode) {
        if (JwtConstants.ERROR_EXPIRED_TOKEN.equals(errorCode)) {
            return ErrorResponse.of(errorCode, "로그아웃 되었습니다. 재로그인 하세요");
        }
        if (JwtConstants.ERROR_INVALID_TOKEN.equals(errorCode)) {
            return ErrorResponse.of(errorCode, "유효하지 않은 토큰입니다.");
        }
        return ErrorResponse.of("인증이 필요합니다.");
    }
}
