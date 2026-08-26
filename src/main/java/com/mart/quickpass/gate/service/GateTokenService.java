package com.mart.quickpass.gate.service;

import com.mart.quickpass.gate.repository.GateTokenRepository;
import com.mart.quickpass.gate.repository.GateTransitionResult;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;

@Service
public class GateTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_COLLISION = "__TOKEN_COLLISION__";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final GateTokenRepository gateTokenRepository;
    private final Clock clock;

    @Autowired
    public GateTokenService(GateTokenRepository gateTokenRepository) {
        this(gateTokenRepository, Clock.system(BUSINESS_ZONE));
    }

    GateTokenService(GateTokenRepository gateTokenRepository, Clock clock) {
        this.gateTokenRepository = gateTokenRepository;
        this.clock = clock;
    }

    public String issue(Long orderId) {
        Instant expiresAt = LocalDate.now(clock).plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        for (int attempt = 0; attempt < 3; attempt++) {
            String issued = gateTokenRepository.issue(orderId, generateToken(), expiresAt);
            if (!TOKEN_COLLISION.equals(issued)) {
                return issued;
            }
        }
        throw new IllegalStateException("Gate Token을 생성하지 못했습니다.");
    }

    /** 결제 취소/환불 확정 흐름에서 호출한다. USED/FAILED는 되돌리지 않고 충돌로 반환한다. */
    public GateTransitionResult revoke(Long orderId) {
        return gateTokenRepository.revoke(orderId);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
