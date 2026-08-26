package com.mart.quickpass.global.config;

import com.mart.quickpass.global.security.JwtAccessDeniedHandler;
import com.mart.quickpass.global.security.JwtAuthenticationEntryPoint;
import com.mart.quickpass.global.security.jwt.JwtTokenProvider;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    void credentialedCorsAllowsOnlyConfiguredOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtTokenProvider.class),
                mock(JwtAuthenticationEntryPoint.class),
                mock(JwtAccessDeniedHandler.class),
                mock(UserRepository.class),
                new AuthCorsProperties(List.of("https://app.quickpass.example"))
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/reissue");
        CorsConfiguration configuration = securityConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.checkOrigin("https://app.quickpass.example"))
                .isEqualTo("https://app.quickpass.example");
        assertThat(configuration.checkOrigin("https://evil.example")).isNull();
    }
}
