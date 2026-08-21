package com.mart.quickpass.auth.event;

import com.mart.quickpass.auth.repository.PasswordResetRepository;
import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.email.client.ResendEmailClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordSecurityEventListenerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetRepository passwordResetRepository;
    @Mock
    private ResendEmailClient resendEmailClient;

    private PasswordSecurityEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PasswordSecurityEventListener(
                refreshTokenRepository,
                passwordResetRepository,
                resendEmailClient
        );
    }

    @Test
    void passwordChangeStoresNewRefreshTokenAfterCommit() {
        listener.handlePasswordChanged(new PasswordChangedEvent(1L, "new-refresh-token"));

        verify(refreshTokenRepository).save(1L, "new-refresh-token");
    }

    @Test
    void committedResetConsumesTokenThenInvalidatesSession() {
        PasswordResetTransactionEvent event =
                new PasswordResetTransactionEvent(1L, "user@example.com", "token-hash");

        listener.completePasswordReset(event);

        InOrder order = inOrder(passwordResetRepository, refreshTokenRepository, resendEmailClient);
        order.verify(passwordResetRepository).completeTokenUse("token-hash");
        order.verify(refreshTokenRepository).deleteByUserId(1L);
        order.verify(resendEmailClient).sendPasswordChangedNotice("user@example.com");
    }

    @Test
    void rolledBackResetReleasesTokenForRetry() {
        listener.rollbackPasswordReset(
                new PasswordResetTransactionEvent(1L, "user@example.com", "token-hash"));

        verify(passwordResetRepository).releaseTokenUse("token-hash");
    }
}
