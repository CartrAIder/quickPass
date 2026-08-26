package com.mart.quickpass.global.security;

import com.mart.quickpass.global.config.GateProperties;
import com.mart.quickpass.global.exception.ErrorCode;
import com.mart.quickpass.global.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor
public class GateServiceSecretFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-AI-Service-Secret";
    private static final String PATH_PREFIX = "/api/internal/gate/";

    private final GateProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String expected = properties.serviceSecret();
        String supplied = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(supplied)
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(ErrorCode.UNAUTHORIZED.httpStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    ErrorResponse.of(ErrorCode.UNAUTHORIZED.name(), "AI Service 인증에 실패했습니다.")));
            return;
        }
        chain.doFilter(request, response);
    }
}
