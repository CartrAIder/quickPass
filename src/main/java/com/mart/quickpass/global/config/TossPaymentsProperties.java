package com.mart.quickpass.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss-payments")
public record TossPaymentsProperties(
        String clientKey,
        String secretKey
) {
}
