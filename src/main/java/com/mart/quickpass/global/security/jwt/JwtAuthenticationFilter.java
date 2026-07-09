package com.mart.quickpass.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            authenticate(request, token);
        }

        filterChain.doFilter(request, response);
    }

    // 토큰을 파싱해 인증 정보를 등록한다. 실패 사유(만료/유효하지 않음)는 요청 속성에 기록해 EntryPoint가 활용한다.
    private void authenticate(HttpServletRequest request, String token) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);

            // 액세스 토큰만 인증에 사용한다. (리프레시 토큰을 액세스 토큰처럼 쓰는 것을 차단)
            if (!jwtTokenProvider.isAccessToken(claims)) {
                request.setAttribute(JwtConstants.TOKEN_ERROR_ATTRIBUTE, JwtConstants.ERROR_INVALID_TOKEN);
                return;
            }

            setAuthentication(request, Long.valueOf(claims.getSubject()));
        } catch (ExpiredJwtException e) {
            // 만료는 별도 코드로 구분해 프론트가 재발급(/reissue)을 시도할 수 있게 한다.
            request.setAttribute(JwtConstants.TOKEN_ERROR_ATTRIBUTE, JwtConstants.ERROR_EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
            request.setAttribute(JwtConstants.TOKEN_ERROR_ATTRIBUTE, JwtConstants.ERROR_INVALID_TOKEN);
        }
    }

    // SecurityContext에 인증 정보를 세팅(principal은 userId)
    private void setAuthentication(HttpServletRequest request, Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // "Authorization: Bearer <token>" 헤더에서 토큰만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtConstants.AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(JwtConstants.TOKEN_PREFIX.length());
        }

        return null;
    }
}
