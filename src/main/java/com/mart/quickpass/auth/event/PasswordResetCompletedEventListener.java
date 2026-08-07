package com.mart.quickpass.auth.event;

import com.mart.quickpass.email.client.ResendEmailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetCompletedEventListener {

    private final ResendEmailClient resendEmailClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PasswordResetCompletedEvent event) {
        try {
            resendEmailClient.sendPasswordChangedNotice(event.email());
        } catch (RuntimeException e) {
            log.error("비밀번호 변경 안내 이메일 전송 실패: email={}", event.email(), e);
        }
    }
}
