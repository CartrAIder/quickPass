package com.mart.quickpass.global.security;

import com.mart.quickpass.global.config.GateProperties;
import com.mart.quickpass.global.exception.ErrorCode;
import com.mart.quickpass.global.exception.ErrorResponse;
import com.mart.quickpass.gate.logging.GateRequestLogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class GateServiceSecretFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-AI-Service-Secret";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
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
        long startedAt = System.nanoTime();
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String requestId = UUID.randomUUID().toString();
        MDC.put(GateRequestLogContext.REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        String authentication = "SUCCESS";
        boolean uncaughtException = false;
        try {
            String expected = properties.serviceSecret();
            String supplied = request.getHeader(HEADER_NAME);
            if (!StringUtils.hasText(expected) || !StringUtils.hasText(supplied)
                    || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    supplied.getBytes(StandardCharsets.UTF_8))) {
                authentication = "REJECTED";
                MDC.put(GateRequestLogContext.ERROR_CODE, ErrorCode.UNAUTHORIZED.name());
                response.setStatus(ErrorCode.UNAUTHORIZED.httpStatus().value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(objectMapper.writeValueAsString(
                        ErrorResponse.of(ErrorCode.UNAUTHORIZED.name(), "AI Service 인증에 실패했습니다.")));
                return;
            }
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException e) {
            uncaughtException = true;
            MDC.put(GateRequestLogContext.ERROR_CODE, ErrorCode.INTERNAL_SERVER_ERROR.name());
            log.error("[AiGate] 처리되지 않은 요청 예외 requestId={}", requestId, e);
            throw e;
        } finally {
            int status = uncaughtException && response.getStatus() < 400
                    ? 500 : response.getStatus();
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("[AiGate] requestId={} method={} path={} status={} durationMs={} auth={} errorCode={} gateId={} tokenRef={} orderId={} transition={} itemCount={} verdict={} failureReason={}",
                    requestId, request.getMethod(), request.getRequestURI(), status, durationMs, authentication,
                    MDC.get(GateRequestLogContext.ERROR_CODE), MDC.get(GateRequestLogContext.GATE_ID),
                    MDC.get(GateRequestLogContext.TOKEN_REF), MDC.get(GateRequestLogContext.ORDER_ID),
                    MDC.get(GateRequestLogContext.TRANSITION), MDC.get(GateRequestLogContext.ITEM_COUNT),
                    MDC.get(GateRequestLogContext.VERDICT), MDC.get(GateRequestLogContext.FAILURE_REASON));
            if (previousContext == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousContext);
            }
        }
    }
}
