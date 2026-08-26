package com.mart.quickpass.auth.service;

import com.mart.quickpass.auth.dto.PasswordResetConfirmRequest;
import com.mart.quickpass.auth.dto.PasswordResetRequest;
import com.mart.quickpass.auth.dto.PasswordResetTokenResponse;
import com.mart.quickpass.auth.event.PasswordResetTransactionEvent;
import com.mart.quickpass.auth.repository.PasswordResetRepository;
import com.mart.quickpass.auth.repository.PasswordResetCodeVerificationResult;
import com.mart.quickpass.email.client.ResendEmailClient;
import com.mart.quickpass.global.config.PasswordResetProperties;
import com.mart.quickpass.global.exception.InvalidPasswordResetCodeException;
import com.mart.quickpass.global.exception.InvalidPasswordResetTokenException;
import com.mart.quickpass.global.exception.PasswordResetCodeExpiredException;
import com.mart.quickpass.global.exception.PasswordResetAttemptsExceededException;
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
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailClient resendEmailClient;
    private final PasswordResetProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    // 재전송 제한을 확인한뒤 가입된 이메일에 6자리 인증 코드를 발송
    public void sendCode(String email) {
        // 동일 이메일의 반복 요청을 제한(1분 제한)
        if (!passwordResetRepository.acquireCooldown(email, properties.resendCooldown())) {
            throw new PasswordResetTooFrequentException();
        }

        // 존재하지 않는 이메일에도 동일한 API 응답을 반환해 계정 존재 여부가 노출되지 않게 한다
        if (!userRepository.existsByEmail(email)) {
            return;
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        try {
            // 이메일 발송에 성공한 코드만 해시 형태로 Redis에 저장한다.
            resendEmailClient.sendPasswordResetCode(email, code);
            passwordResetRepository.saveCode(email, hash(code), properties.codeTtl());
        } catch (RuntimeException e) {
            // 발송 또는 저장 실패 시 사용자가 즉시 다시 요청할 수 있도록 쿨다운을 제거한다.
            passwordResetRepository.deleteCooldown(email);
            throw e;
        }
    }

    // 이메일 인증 코드를 검증하고 비밀번호 변경에 사용할 일회용 재설정 토큰을 발급한다.
    public PasswordResetTokenResponse confirmCode(PasswordResetConfirmRequest request) {
        // 원문 인증 코드 대신 해시를 비교해 Redis 유출 시 코드 노출 위험을 줄인다.
        String requestedHash = hash(request.code());
        PasswordResetCodeVerificationResult result =
                passwordResetRepository.verifyCode(request.email(), requestedHash);

        // 저장소의 검증 결과를 각 비즈니스 예외로 변환한다.
        switch (result) {
            case EXPIRED -> throw new PasswordResetCodeExpiredException();
            case INVALID -> throw new InvalidPasswordResetCodeException();
            case ATTEMPTS_EXCEEDED -> throw new PasswordResetAttemptsExceededException();
            case SUCCESS -> {
                // 계속 진행
            }
        }

        String resetToken = createResetToken();
        // 클라이언트에는 원문 토큰을 전달하고 서버에는 해시만 제한 시간 동안 저장한다.
        passwordResetRepository.saveToken(hash(resetToken), request.email(), properties.tokenTtl());

        return new PasswordResetTokenResponse(resetToken, properties.tokenTtl().toSeconds());
    }

    // 재설정 토큰을 선점한 뒤 비밀번호를 변경하고 트랜잭션 결과에 따른 후속 처리를 예약한다.
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String tokenHash = hash(request.resetToken());

        // 동일 토큰의 동시·중복 사용을 막기 위해 검증과 함께 토큰 사용권을 선점한다.
        String email = passwordResetRepository.acquireTokenUse(tokenHash)
                .orElseThrow(InvalidPasswordResetTokenException::new);

        // 토큰에 연결된 사용자가 없으면 선점을 해제하고 유효하지 않은 요청으로 처리한다.
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            passwordResetRepository.releaseTokenUse(tokenHash);
            throw new InvalidPasswordResetTokenException();
        });

        // 커밋 시 토큰 소비·기존 세션 폐기·안내 메일을 수행하고, 롤백 시 토큰 선점을 해제한다.
        eventPublisher.publishEvent(new PasswordResetTransactionEvent(user.getId(), email, tokenHash));

        // 영속 Entity의 변경은 트랜잭션 커밋 시 JPA 변경 감지로 MySQL에 반영된다.
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    // URL에 안전하게 전달할 수 있는 256비트 일회용 재설정 토큰을 생성한다.
    private String createResetToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // 인증 정보의 원문을 저장하지 않도록 SHA-256 해시를 16진수 문자열로 변환한다.
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
