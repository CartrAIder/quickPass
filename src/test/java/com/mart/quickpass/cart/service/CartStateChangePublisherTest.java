package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CartStateChangePublisherTest {

    @Test
    void incrementsVersionBeforePublishingEvent() {
        CartVersionRepository versionRepository = mock(CartVersionRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CartStateChangePublisher publisher = new CartStateChangePublisher(versionRepository, eventPublisher);
        when(versionRepository.increment("CART-001")).thenReturn(11L);

        publisher.publish(42L, "CART-001", CartChangeType.UPDATED);

        InOrder order = inOrder(versionRepository, eventPublisher);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        order.verify(versionRepository).increment("CART-001");
        order.verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOfSatisfying(CartChangedEvent.class, changed -> {
            assertThat(changed.version()).isEqualTo(11L);
            assertThat(changed.type()).isEqualTo(CartChangeType.UPDATED);
        });
    }

    @Test
    void doesNotPublishEventWhenVersionIncrementFails() {
        CartVersionRepository versionRepository = mock(CartVersionRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CartStateChangePublisher publisher = new CartStateChangePublisher(versionRepository, eventPublisher);
        when(versionRepository.increment("CART-001"))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        assertThatThrownBy(() -> publisher.publish(42L, "CART-001", CartChangeType.UPDATED))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(eventPublisher);
    }

}
