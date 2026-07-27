package com.mart.quickpass.payment.service;

import com.mart.quickpass.global.config.TossPaymentsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** 토스페이먼츠 승인 API를 호출하는 서버 전용 클라이언트다. */
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private static final String BASE_URL = "https://api.tosspayments.com";

    private final TossPaymentsProperties properties;

    public TossPaymentApprovalResult approve(String paymentKey, String orderId, Long amount) {
        if (!StringUtils.hasText(properties.secretKey())) {
            return TossPaymentApprovalResult.failure(
                    "CONFIGURATION_ERROR",
                    "토스페이먼츠 시크릿 키가 설정되지 않았습니다."
            );
        }

        try {
            TossPaymentConfirmResponse response = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeaders(headers -> headers.setBasicAuth(properties.secretKey(), ""))
                    .build()
                    .post()
                    .uri("/v1/payments/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TossPaymentConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossPaymentConfirmResponse.class);

            if (response == null) {
                return TossPaymentApprovalResult.failure("EMPTY_RESPONSE", "토스페이먼츠 승인 응답이 비어 있습니다.");
            }
            return TossPaymentApprovalResult.success(response);
        } catch (RestClientResponseException e) {
            return TossPaymentApprovalResult.failure(
                    "TOSS_" + e.getStatusCode().value(),
                    truncate(e.getResponseBodyAsString())
            );
        } catch (RestClientException e) {
            // DNS/timeout/connection reset 등은 결제 시도에 FAILED로 기록되어 사용자에게 재시도를 안내한다.
            return TossPaymentApprovalResult.failure("COMMUNICATION_ERROR", truncate(e.getMessage()));
        }
    }

    private String truncate(String message) {
        if (message == null) return null;
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private record TossPaymentConfirmRequest(String paymentKey, String orderId, Long amount) { }

    public record TossPaymentConfirmResponse(
            String paymentKey, String orderId, Long totalAmount, String status, String method
    ) { }

    public record TossPaymentApprovalResult(
            TossPaymentConfirmResponse response, String failureCode, String failureMessage
    ) {
        public static TossPaymentApprovalResult success(TossPaymentConfirmResponse response) {
            return new TossPaymentApprovalResult(response, null, null);
        }

        public static TossPaymentApprovalResult failure(String failureCode, String failureMessage) {
            return new TossPaymentApprovalResult(null, failureCode, failureMessage);
        }

        public boolean isSuccess() {
            return response != null;
        }
    }
}
