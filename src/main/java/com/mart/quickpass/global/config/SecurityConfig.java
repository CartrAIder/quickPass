package com.mart.quickpass.global.config;

import com.mart.quickpass.global.security.JwtAuthenticationEntryPoint;
import com.mart.quickpass.global.security.jwt.JwtAuthenticationFilter;
import com.mart.quickpass.global.security.jwt.JwtConstants;
import com.mart.quickpass.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final String[] PERMIT_ALL_PATTERNS = {
            "/api/users/signup",
            "/api/auth/login",
            "/api/auth/reissue",
            "/api/auth/logout",
            "/api/mobile/auth/login",
            "/api/mobile/auth/reissue",
            "/api/mobile/auth/logout",
            // SSE 구독은 액세스 토큰 대신 단명 티켓으로 인증한다(컨트롤러에서 검증). 티켓 발급(/sse-ticket)은 인증 필요.
            "/api/carts/subscribe",
            // 토스 결제 완료 후 successUrl/failUrl로 브라우저가 직접 이동한다.
            // 승인 API 자체는 아래 예외에 포함되지 않으므로 JWT 인증이 계속 필요하다.
            "/payments/**",
            "/error",
            // Swagger UI / OpenAPI 문서
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    // 비밀번호 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 개발 단계 임시 설정, 배포 시 프론트 도메인 기준으로 좁힐
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        // 로그인 응답의 Access Token 헤더를 크로스오리진 JS가 읽을 수 있도록 노출
        configuration.setExposedHeaders(List.of(JwtConstants.AUTHORIZATION_HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
