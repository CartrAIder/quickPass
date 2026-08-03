package com.mart.quickpass.payment.repository;

import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByPaymentAttemptId(String paymentAttemptId);

    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);
}
