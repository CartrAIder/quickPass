package com.mart.quickpass.global.security.jwt;

import com.mart.quickpass.global.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(new JwtProperties(
                "01234567890123456789012345678901",
                60_000,
                120_000
        ));
    }

    @Test
    void validateTokenRejectsMalformedToken() {
        assertThat(jwtTokenProvider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateTokenRejectsUnsupportedUnsecuredJwt() {
        String unsecuredJwt = "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIiwidHlwZSI6InJlZnJlc2gifQ.";

        assertThat(jwtTokenProvider.validateToken(unsecuredJwt)).isFalse();
    }

    @Test
    void validateTokenAcceptsIssuedRefreshToken() {
        assertThat(jwtTokenProvider.validateToken(jwtTokenProvider.createRefreshToken(1L))).isTrue();
    }
}
