package com.mart.quickpass.gate.logging;

import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** AI 게이트 요청의 로그 필드를 한 요청 동안만 보관한다. */
public final class GateRequestLogContext {

    public static final String REQUEST_ID = "gateRequestId";
    public static final String ERROR_CODE = "gateErrorCode";
    public static final String GATE_ID = "gateId";
    public static final String TOKEN_REF = "gateTokenRef";
    public static final String ORDER_ID = "gateOrderId";
    public static final String TRANSITION = "gateTransition";
    public static final String ITEM_COUNT = "gateItemCount";
    public static final String VERDICT = "gateVerdict";
    public static final String FAILURE_REASON = "gateFailureReason";

    private GateRequestLogContext() {
    }

    public static void put(String key, String value) {
        if (MDC.get(REQUEST_ID) != null) {
            MDC.put(key, value);
        }
    }

    public static void putGateId(String gateId) {
        put(GATE_ID, gateId.replaceAll("[\\p{Cntrl}]", "_"));
    }

    /** 원본 토큰을 노출하지 않고 검사 시작과 결과 요청을 연결한다. */
    public static void putTokenReference(String token) {
        if (MDC.get(REQUEST_ID) == null) {
            return;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            put(TOKEN_REF, HexFormat.of().formatHex(digest, 0, 8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
