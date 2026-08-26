package com.mart.quickpass.auth.event;

import com.mart.quickpass.auth.repository.PasswordResetRepository;
import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.email.client.ResendEmailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordSecurityEventListener {

    // DB 트랜잭션 완료 후 수행하는 Redis 후속 작업의 일시적인 실패를 보완한다.
    private static final int REDIS_RETRY_COUNT = 3;

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final ResendEmailClient resendEmailClient;

    // 비밀번호 변경이 DB에 정상 반영된 경우에만 새 Refresh Token으로 교체한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordChanged(PasswordChangedEvent event) {
        retryRedis("비밀번호 변경 후 Refresh Token 교체", event.userId(),
                () -> refreshTokenRepository.save(event.userId(), event.refreshToken()));
    }

    // 비밀번호 재설정 트랜잭션 커밋 후 일회용 토큰을 소비하고 기존 로그인 세션을 만료시킨다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void completePasswordReset(PasswordResetTransactionEvent event) {
        retryRedis("비밀번호 재설정 토큰 소비", event.userId(),
                () -> passwordResetRepository.completeTokenUse(event.tokenHash()));
        retryRedis("비밀번호 재설정 후 Refresh Token 폐기", event.userId(),
                () -> refreshTokenRepository.deleteByUserId(event.userId()));

        // 안내 이메일 실패가 이미 커밋된 비밀번호 재설정 결과에 영향을 주지 않도록 예외를 기록한다.
        try {
            resendEmailClient.sendPasswordChangedNotice(event.email());
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 안내 이메일 전송 실패: email={}", event.email(), e);
        }
    }

    // DB 작업이 롤백되면 선점해 둔 재설정 토큰을 다시 사용할 수 있도록 잠금을 해제한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void rollbackPasswordReset(PasswordResetTransactionEvent event) {
        retryRedis("비밀번호 재설정 토큰 잠금 해제", event.userId(),
                () -> passwordResetRepository.releaseTokenUse(event.tokenHash()));
    }

    // Redis 장애가 일시적일 수 있으므로 작업을 정해진 횟수만큼 재시도하고 최종 실패를 기록한다.
    private void retryRedis(String operation, Long userId, Runnable action) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= REDIS_RETRY_COUNT; attempt++) {
            try {
                action.run();
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                log.warn("{} 실패: userId={}, attempt={}/{}", operation, userId, attempt, REDIS_RETRY_COUNT, e);
            }
        }
        log.error("{} 최종 실패: userId={}", operation, userId, lastFailure);
    }
}
