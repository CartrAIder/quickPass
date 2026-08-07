package com.mart.quickpass.auth.service;

import com.mart.quickpass.auth.dto.AuthTokens;
import com.mart.quickpass.auth.dto.ChangePasswordRequest;
import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.global.exception.CurrentPasswordMismatchException;
import com.mart.quickpass.global.security.jwt.JwtTokenProvider;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
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
    private User user;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenRepository
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

        AuthTokens result = authService.changePassword(userId, request);

        assertThat(result).isEqualTo(new AuthTokens("new-access-token", "new-refresh-token", "홍길동"));
        verify(user).changePassword("new-encoded-password");

        InOrder tokenRotation = inOrder(refreshTokenRepository);
        tokenRotation.verify(refreshTokenRepository).deleteByUserId(userId);
        tokenRotation.verify(refreshTokenRepository).save(userId, "new-refresh-token");
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
    }
}
