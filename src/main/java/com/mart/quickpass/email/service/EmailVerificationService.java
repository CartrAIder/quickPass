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

    // 랜덤 인증번호 생성
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ResendEmailClient resendEmailClient;
    private final EmailVerificationProperties properties;

    // 인증번호 발송 메서드
    public void sendCode(String email) {
        if (userRepository.existsByEmail(email)) { // 이메일 중복 확인
            throw new DuplicateEmailException(email);
        }
        if (!emailVerificationRepository.acquireCooldown(email, properties.resendCooldown())) { // 재발송 제한 확인
            throw new EmailVerificationTooFrequentException();
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        try {
            // 이메일 발송 및 인증번호 저장
            resendEmailClient.sendVerificationCode(email, code);
            emailVerificationRepository.saveCode(email, hash(code), properties.codeTtl());
        } catch (RuntimeException e) {
            // Resend 전송 또는 Redis 저장 실패 시 사용자가 즉시 다시 시도할 수 있게 한다(cooldown 삭제)
            emailVerificationRepository.deleteCooldown(email);
            throw e;
        }
    }

    // 인증번호 검사 메서드
    public void verifyCode(String email, String code) {
        // 저장된 인증번호 해시 조회
        String storedHash = emailVerificationRepository.findCodeHash(email)
                .orElseThrow(EmailVerificationExpiredException::new);

        // 저장된 해시와 전송된 해시 비교
        if (!MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                hash(code).getBytes(StandardCharsets.UTF_8)
        )) {
            throw new InvalidEmailVerificationCodeException();
        }

        // 사용한 인증번호 삭제 및 인증 완료 상태 저장
        emailVerificationRepository.deleteCode(email);
        emailVerificationRepository.markVerified(email, properties.verifiedTtl());
    }

    public void requireVerified(String email) {
        if (!emailVerificationRepository.isVerified(email)) {
            throw new EmailNotVerifiedException();
        }
    }

    // 이메일 인증 여부 검사
    public void consumeVerification(String email) {
        emailVerificationRepository.consumeVerified(email);
    }

    // 문자열을 SHA-256 해시 문자열로 변환
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
