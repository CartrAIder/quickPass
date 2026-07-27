package com.mart.quickpass.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cookie")
public record AuthCookieProperties(
        boolean secure
) {
}
