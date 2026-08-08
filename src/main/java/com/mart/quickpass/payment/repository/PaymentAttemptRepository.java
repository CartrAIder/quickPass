package com.mart.quickpass.payment.repository;

import com.mart.quickpass.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import com.mart.quickpass.payment.entity.PaymentStatus;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    boolean existsByOrderUserIdAndStatusIn(Long userId, Collection<PaymentStatus> statuses);

    Optional<PaymentAttempt> findByPaymentAttemptId(String paymentAttemptId);
}
