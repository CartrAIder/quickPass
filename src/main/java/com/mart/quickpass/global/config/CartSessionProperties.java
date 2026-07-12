package com.mart.quickpass.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cart.session")
public record CartSessionProperties(
        Duration ttl
) {
}
