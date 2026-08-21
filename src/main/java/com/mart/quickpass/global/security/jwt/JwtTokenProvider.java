package com.mart.quickpass.global.security.jwt;

import com.mart.quickpass.global.config.JwtProperties;
import com.mart.quickpass.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, UserRole role) {
        return createToken(userId, role, TYPE_ACCESS, jwtProperties.accessTokenValidity());
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, null, TYPE_REFRESH, jwtProperties.refreshTokenValidity());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 클라이언트 입력 오류이므로 원문 토큰 없이 낮은 로그 수준으로 기록한다.
            log.debug("JWT 검증 실패: type={}", e.getClass().getSimpleName());
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    // 액세스 토큰 여부 (이미 파싱된 claims 기준)
    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    // 리프레시 토큰 여부
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    private String createToken(Long userId, UserRole role, String type, long validityMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey);

        if (role != null) {
            builder.claim(CLAIM_ROLE, role.name());
        }

        return builder.compact();
    }
}
