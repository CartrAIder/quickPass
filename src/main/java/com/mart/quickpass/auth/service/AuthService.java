package com.mart.quickpass.auth.service;

import com.mart.quickpass.auth.dto.AuthTokens;
import com.mart.quickpass.auth.dto.LoginRequest;
import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.global.exception.InvalidCredentialsException;
import com.mart.quickpass.global.exception.InvalidTokenException;
import com.mart.quickpass.global.security.jwt.JwtTokenProvider;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 로그인 메서드
    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    // 토큰 재발급 메서드 (Refresh Token Rotation)
    @Transactional(readOnly = true)
    public AuthTokens reissue(String refreshToken) {
        // 유효하고, 타입이 refresh인 토큰만 허용 (액세스 토큰으로 재발급 시도 차단)
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        String storedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(InvalidTokenException::new);

        // 쿠키의 토큰과 저장된 토큰이 다르면 탈취 가능성 → 저장된 토큰 무효화
        if (!storedToken.equals(refreshToken)) {
            refreshTokenRepository.deleteByUserId(userId);
            throw new InvalidTokenException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidTokenException::new);

        // 기존 리프레시 토큰을 새 토큰으로 교체(회전)하여 재발급
        return issueTokens(user);
    }

    // 로그아웃 메서드 - 저장된 리프레시 토큰 삭제
    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return;
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        refreshTokenRepository.deleteByUserId(userId);
    }

    // 토큰 발급 + 리프레시 토큰 저장(기존 값 덮어쓰기 = 회전)
    private AuthTokens issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(user.getId(), refreshToken);

        return new AuthTokens(accessToken, refreshToken);
    }
}
