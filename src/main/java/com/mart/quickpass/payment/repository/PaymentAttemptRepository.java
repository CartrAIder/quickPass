package com.mart.quickpass.payment.repository;

import com.mart.quickpass.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByPaymentAttemptId(String paymentAttemptId);
}
