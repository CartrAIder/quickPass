package com.mart.quickpass.email.service;

import com.mart.quickpass.email.client.ResendEmailClient;
import com.mart.quickpass.email.exception.EmailNotVerifiedException;
import com.mart.quickpass.email.exception.EmailVerificationExpiredException;
import com.mart.quickpass.email.exception.EmailVerificationTooFrequentException;
import com.mart.quickpass.email.exception.InvalidEmailVerificationCodeException;
import com.mart.quickpass.email.repository.EmailVerificationRepository;
import com.mart.quickpass.global.config.EmailVerificationProperties;
import com.mart.quickpass.global.exception.DuplicateEmailException;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ResendEmailClient resendEmailClient;
    private final EmailVerificationProperties properties;

    public void sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        if (!emailVerificationRepository.acquireCooldown(email, properties.resendCooldown())) {
            throw new EmailVerificationTooFrequentException();
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        try {
            resendEmailClient.sendVerificationCode(email, code);
            emailVerificationRepository.saveCode(email, hash(code), properties.codeTtl());
        } catch (RuntimeException e) {
            // Resend 전송 또는 Redis 저장 실패 시 사용자가 즉시 다시 시도할 수 있게 한다.
            emailVerificationRepository.deleteCooldown(email);
            throw e;
        }
    }

    public void verifyCode(String email, String code) {
        String storedHash = emailVerificationRepository.findCodeHash(email)
                .orElseThrow(EmailVerificationExpiredException::new);

        if (!MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                hash(code).getBytes(StandardCharsets.UTF_8)
        )) {
            throw new InvalidEmailVerificationCodeException();
        }

        emailVerificationRepository.deleteCode(email);
        emailVerificationRepository.markVerified(email, properties.verifiedTtl());
    }

    public void requireVerified(String email) {
        if (!emailVerificationRepository.isVerified(email)) {
            throw new EmailNotVerifiedException();
        }
    }

    public void consumeVerification(String email) {
        emailVerificationRepository.consumeVerified(email);
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
