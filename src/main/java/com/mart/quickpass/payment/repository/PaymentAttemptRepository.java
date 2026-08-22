package com.mart.quickpass.payment.repository;

import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    boolean existsByOrderUserIdAndStatusIn(Long userId, Collection<PaymentStatus> statuses);

    Optional<PaymentAttempt> findByPaymentAttemptId(String paymentAttemptId);

    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);

    List<PaymentAttempt> findAllByOrder_IdAndStatus(Long orderId, PaymentStatus status);
}
