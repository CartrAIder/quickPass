package com.mart.quickpass.auth.service;

import com.mart.quickpass.auth.dto.AuthResult;
import com.mart.quickpass.auth.dto.ChangePasswordRequest;
import com.mart.quickpass.auth.dto.LoginRequest;
import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.auth.event.PasswordChangedEvent;
import com.mart.quickpass.global.exception.CurrentPasswordMismatchException;
import com.mart.quickpass.global.exception.InvalidTokenException;
import com.mart.quickpass.global.exception.InvalidCredentialsException;
import com.mart.quickpass.global.security.jwt.JwtTokenProvider;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private User user;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenRepository,
                eventPublisher
        );
    }

    @Test
    void changePasswordReplacesPasswordAndTokens() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("Current1!", "Changed1!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getPassword()).thenReturn("old-encoded-password");
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getName()).thenReturn("홍길동");
        when(passwordEncoder.matches(request.currentPassword(), "old-encoded-password")).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn("new-encoded-password");
        when(jwtTokenProvider.createAccessToken(userId, UserRole.USER)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(userId)).thenReturn("new-refresh-token");

        AuthResult result = authService.changePassword(userId, request);

        assertThat(result).isEqualTo(new AuthResult("new-access-token", "new-refresh-token", "홍길동"));
        verify(user).changePassword("new-encoded-password");

        verify(eventPublisher).publishEvent(new PasswordChangedEvent(userId, "new-refresh-token"));
        verify(refreshTokenRepository, never()).deleteByUserId(userId);
        verify(refreshTokenRepository, never()).save(userId, "new-refresh-token");
    }

    @Test
    void loginIssuesTokensForActiveUserWithMatchingPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.getPassword()).thenReturn("encoded-password");
        when(passwordEncoder.matches(request.password(), "encoded-password")).thenReturn(true);
        when(user.getId()).thenReturn(1L);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getName()).thenReturn("홍길동");
        when(jwtTokenProvider.createAccessToken(1L, UserRole.USER)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");

        AuthResult result = authService.login(request);

        assertThat(result).isEqualTo(new AuthResult("access-token", "refresh-token", "홍길동"));
        verify(refreshTokenRepository).save(1L, "refresh-token");
    }

    @Test
    void loginRejectsIncorrectPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "Wrong1!");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.getPassword()).thenReturn("encoded-password");
        when(passwordEncoder.matches(request.password(), "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenRepository, never()).save(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void loginRejectsWithdrawnUser() {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(refreshTokenRepository, never()).save(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest("Wrong1!", "Changed1!");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getPassword()).thenReturn("old-encoded-password");
        when(passwordEncoder.matches(request.currentPassword(), "old-encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(userId, request))
                .isInstanceOf(CurrentPasswordMismatchException.class);

        verify(user, never()).changePassword(org.mockito.ArgumentMatchers.anyString());
        verify(refreshTokenRepository, never()).deleteByUserId(userId);
        verify(refreshTokenRepository, never()).save(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reissueRejectsPreviousTokenWithoutDeletingCurrentToken() {
        Long userId = 1L;
        String previousToken = "previous-refresh-token";
        when(jwtTokenProvider.validateToken(previousToken)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(previousToken)).thenReturn(true);
        when(jwtTokenProvider.getUserId(previousToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getName()).thenReturn("홍길동");
        when(jwtTokenProvider.createAccessToken(userId, UserRole.USER)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(userId)).thenReturn("new-refresh-token");
        when(refreshTokenRepository.rotateIfMatches(userId, previousToken, "new-refresh-token"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.reissue(previousToken))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository, never()).deleteByUserId(userId);
        verify(refreshTokenRepository, never()).save(userId, "new-refresh-token");
    }

    @Test
    void reissueAtomicallyRotatesMatchingCurrentToken() {
        Long userId = 1L;
        String currentToken = "current-refresh-token";
        when(jwtTokenProvider.validateToken(currentToken)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(currentToken)).thenReturn(true);
        when(jwtTokenProvider.getUserId(currentToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.getId()).thenReturn(userId);
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.getName()).thenReturn("홍길동");
        when(jwtTokenProvider.createAccessToken(userId, UserRole.USER)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(userId)).thenReturn("new-refresh-token");
        when(refreshTokenRepository.rotateIfMatches(userId, currentToken, "new-refresh-token"))
                .thenReturn(true);

        AuthResult result = authService.reissue(currentToken);

        assertThat(result).isEqualTo(new AuthResult("new-access-token", "new-refresh-token", "홍길동"));
        verify(refreshTokenRepository).rotateIfMatches(userId, currentToken, "new-refresh-token");
        verify(refreshTokenRepository, never()).save(userId, "new-refresh-token");
    }

    @Test
    void logoutRejectsPreviousTokenWithoutDeletingCurrentToken() {
        Long userId = 1L;
        String previousToken = "previous-refresh-token";
        when(jwtTokenProvider.validateToken(previousToken)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(previousToken)).thenReturn(true);
        when(jwtTokenProvider.getUserId(previousToken)).thenReturn(userId);
        when(refreshTokenRepository.deleteIfMatches(userId, previousToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.logout(previousToken))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository, never()).deleteByUserId(userId);
    }

    @Test
    void logoutDeletesMatchingCurrentToken() {
        Long userId = 1L;
        String currentToken = "current-refresh-token";
        when(jwtTokenProvider.validateToken(currentToken)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(currentToken)).thenReturn(true);
        when(jwtTokenProvider.getUserId(currentToken)).thenReturn(userId);
        when(refreshTokenRepository.deleteIfMatches(userId, currentToken)).thenReturn(true);

        authService.logout(currentToken);

        verify(refreshTokenRepository).deleteIfMatches(userId, currentToken);
    }
}
