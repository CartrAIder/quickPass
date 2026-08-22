package com.mart.quickpass.auth.service;

import com.mart.quickpass.auth.dto.AuthResult;
import com.mart.quickpass.auth.dto.ChangePasswordRequest;
import com.mart.quickpass.auth.dto.LoginRequest;
import com.mart.quickpass.auth.event.PasswordChangedEvent;
import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.global.exception.CurrentPasswordMismatchException;
import com.mart.quickpass.global.exception.InvalidCredentialsException;
import com.mart.quickpass.global.exception.InvalidTokenException;
import com.mart.quickpass.global.exception.UserNotFoundException;
import com.mart.quickpass.global.security.jwt.JwtTokenProvider;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    // 로그인 메서드
    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        // 이메일 기반 유저 탐색
        User user = userRepository.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        // 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    // 토큰 재발급 메서드
    public AuthResult reissue(String refreshToken) {
        // 유효하고, 타입이 refresh인 토큰만 허용
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(InvalidTokenException::new);

        AuthResult newTokens = createTokens(user);
        if (!refreshTokenRepository.rotateIfMatches(userId, refreshToken, newTokens.refreshToken())) {
            throw new InvalidTokenException();
        }

        return newTokens;
    }

    // 로그아웃 메서드 - 저장된 리프레시 토큰 삭제
    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        if (!refreshTokenRepository.deleteIfMatches(userId, refreshToken)) {
            throw new InvalidTokenException();
        }
    }

    // 비밀번호 변경 메서드(로그인 후 변경)
    @Transactional
    public AuthResult changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 기존의 비밀번호가 사용자의 입력과 일치하는지 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CurrentPasswordMismatchException();
        }

        // 새 비밀번호 적용
        user.changePassword(passwordEncoder.encode(request.newPassword()));

        // 새로운 jwt 발급 및 Refresh Token 교체 이벤트 발행
        AuthResult tokens = createTokens(user);
        eventPublisher.publishEvent(new PasswordChangedEvent(userId, tokens.refreshToken()));
        return tokens;
    }

    // 토큰 발급 및 리프레시 토큰 저장 메서드
    private AuthResult issueTokens(User user) {
        AuthResult tokens = createTokens(user);
        refreshTokenRepository.save(user.getId(), tokens.refreshToken());
        return tokens;
    }

    // 토큰 생성 메서드
    private AuthResult createTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new AuthResult(accessToken, refreshToken, user.getName());
    }
}
