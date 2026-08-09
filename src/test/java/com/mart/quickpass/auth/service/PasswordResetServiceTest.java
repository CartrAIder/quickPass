package com.mart.quickpass.auth.service;

import com.mart.quickpass.auth.dto.PasswordResetConfirmRequest;
import com.mart.quickpass.auth.dto.PasswordResetRequest;
import com.mart.quickpass.auth.dto.PasswordResetTokenResponse;
import com.mart.quickpass.auth.event.PasswordResetCompletedEvent;
import com.mart.quickpass.auth.repository.PasswordResetRepository;
import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.email.client.ResendEmailClient;
import com.mart.quickpass.global.config.PasswordResetProperties;
import com.mart.quickpass.global.exception.InvalidPasswordResetCodeException;
import com.mart.quickpass.global.exception.InvalidPasswordResetTokenException;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofMinutes(1);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetRepository passwordResetRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ResendEmailClient resendEmailClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private User user;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                passwordResetRepository,
                refreshTokenRepository,
                passwordEncoder,
                resendEmailClient,
                new PasswordResetProperties(CODE_TTL, TOKEN_TTL, COOLDOWN),
                eventPublisher
        );
    }

    @Test
    void sendCodeSendsOnlyForRegisteredEmail() {
        String email = "user@example.com";
        when(passwordResetRepository.acquireCooldown(email, COOLDOWN)).thenReturn(true);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        passwordResetService.sendCode(email);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(resendEmailClient).sendPasswordResetCode(org.mockito.ArgumentMatchers.eq(email), codeCaptor.capture());
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        verify(passwordResetRepository).saveCode(
                org.mockito.ArgumentMatchers.eq(email),
                org.mockito.ArgumentMatchers.matches("[0-9a-f]{64}"),
                org.mockito.ArgumentMatchers.eq(CODE_TTL)
        );
    }

    @Test
    void sendCodeKeepsSameSuccessfulFlowForUnknownEmail() {
        String email = "unknown@example.com";
        when(passwordResetRepository.acquireCooldown(email, COOLDOWN)).thenReturn(true);
        when(userRepository.existsByEmail(email)).thenReturn(false);

        passwordResetService.sendCode(email);

        verify(resendEmailClient, never()).sendPasswordResetCode(anyString(), anyString());
        verify(passwordResetRepository, never()).saveCode(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class)
        );
    }

    @Test
    void confirmCodeConsumesCodeAndIssuesFiveMinuteToken() {
        String email = "user@example.com";
        String code = "123456";
        String codeHash = hash(code);
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(email, code);
        when(passwordResetRepository.findCodeHash(email)).thenReturn(Optional.of(codeHash));
        when(passwordResetRepository.consumeCode(email, codeHash)).thenReturn(true);

        PasswordResetTokenResponse response = passwordResetService.confirmCode(request);

        assertThat(response.resetToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(300);
        verify(passwordResetRepository).saveToken(hash(response.resetToken()), email, TOKEN_TTL);
    }

    @Test
    void confirmCodeRejectsIncorrectCode() {
        String email = "user@example.com";
        when(passwordResetRepository.findCodeHash(email)).thenReturn(Optional.of(hash("123456")));

        assertThatThrownBy(() -> passwordResetService.confirmCode(
                new PasswordResetConfirmRequest(email, "654321")
        )).isInstanceOf(InvalidPasswordResetCodeException.class);

        verify(passwordResetRepository, never()).consumeCode(anyString(), anyString());
        verify(passwordResetRepository, never()).saveToken(
                anyString(),
                anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class)
        );
    }

    @Test
    void resetPasswordConsumesTokenAndInvalidatesRefreshToken() {
        String email = "user@example.com";
        String resetToken = "reset-token";
        String tokenHash = hash(resetToken);
        Long userId = 1L;
        when(passwordResetRepository.findEmailByToken(tokenHash)).thenReturn(Optional.of(email));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Changed1!")).thenReturn("encoded-new-password");
        when(passwordResetRepository.consumeToken(tokenHash)).thenReturn(Optional.of(email));
        when(user.getId()).thenReturn(userId);

        passwordResetService.resetPassword(new PasswordResetRequest(resetToken, "Changed1!"));

        verify(user).changePassword("encoded-new-password");
        InOrder completionOrder = inOrder(userRepository, passwordResetRepository, refreshTokenRepository);
        completionOrder.verify(userRepository).saveAndFlush(user);
        completionOrder.verify(passwordResetRepository).consumeToken(tokenHash);
        completionOrder.verify(refreshTokenRepository).deleteByUserId(userId);
        verify(eventPublisher).publishEvent(new PasswordResetCompletedEvent(email));
    }

    @Test
    void resetPasswordRejectsAlreadyConsumedToken() {
        String email = "user@example.com";
        String resetToken = "reset-token";
        String tokenHash = hash(resetToken);
        when(passwordResetRepository.findEmailByToken(tokenHash)).thenReturn(Optional.of(email));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Changed1!")).thenReturn("encoded-new-password");
        when(passwordResetRepository.consumeToken(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new PasswordResetRequest(resetToken, "Changed1!")
        )).isInstanceOf(InvalidPasswordResetTokenException.class);

        verify(refreshTokenRepository, never()).deleteByUserId(org.mockito.ArgumentMatchers.anyLong());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
