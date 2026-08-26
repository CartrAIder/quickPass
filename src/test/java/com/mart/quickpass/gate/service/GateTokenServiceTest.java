package com.mart.quickpass.gate.service;

import com.mart.quickpass.gate.repository.GateTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GateTokenServiceTest {

    @Test
    void tokenExpiresAtNextSeoulMidnight() {
        GateTokenRepository repository = mock(GateTokenRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-23T14:59:00Z"), ZoneId.of("Asia/Seoul"));
        GateTokenService service = new GateTokenService(repository, clock);
        ArgumentCaptor<Instant> expiry = ArgumentCaptor.forClass(Instant.class);
        when(repository.issue(eq(10L), anyString(), expiry.capture())).thenReturn("existing-or-new-token");

        assertThat(service.issue(10L)).isEqualTo("existing-or-new-token");
        assertThat(expiry.getValue()).isEqualTo(Instant.parse("2026-08-23T15:00:00Z"));
    }
}
