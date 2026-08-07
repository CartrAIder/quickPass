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
import com.mart.quickpass.global.exception.PasswordResetCodeExpiredException;
import com.mart.quickpass.global.exception.PasswordResetTooFrequentException;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RESET_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailClient resendEmailClient;
    private final PasswordResetProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    public void sendCode(String email) {
        if (!passwordResetRepository.acquireCooldown(email, properties.resendCooldown())) {
            throw new PasswordResetTooFrequentException();
        }

        // 계정 존재 여부와 관계없이 컨트롤러는 같은 응답을 반환한다.
        if (!userRepository.existsByEmail(email)) {
            return;
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        try {
            resendEmailClient.sendPasswordResetCode(email, code);
            passwordResetRepository.saveCode(email, hash(code), properties.codeTtl());
        } catch (RuntimeException e) {
            passwordResetRepository.deleteCooldown(email);
            throw e;
        }
    }

    public PasswordResetTokenResponse confirmCode(PasswordResetConfirmRequest request) {
        String storedHash = passwordResetRepository.findCodeHash(request.email())
                .orElseThrow(PasswordResetCodeExpiredException::new);
        String requestedHash = hash(request.code());

        if (!MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                requestedHash.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new InvalidPasswordResetCodeException();
        }

        if (!passwordResetRepository.consumeCode(request.email(), requestedHash)) {
            throw new PasswordResetCodeExpiredException();
        }

        String resetToken = createResetToken();
        passwordResetRepository.saveToken(hash(resetToken), request.email(), properties.tokenTtl());

        return new PasswordResetTokenResponse(resetToken, properties.tokenTtl().toSeconds());
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String tokenHash = hash(request.resetToken());
        String email = passwordResetRepository.findEmailByToken(tokenHash)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);

        String consumedEmail = passwordResetRepository.consumeToken(tokenHash)
                .orElseThrow(InvalidPasswordResetTokenException::new);
        if (!email.equals(consumedEmail)) {
            throw new InvalidPasswordResetTokenException();
        }

        refreshTokenRepository.deleteByUserId(user.getId());
        eventPublisher.publishEvent(new PasswordResetCompletedEvent(email));
    }

    private String createResetToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
