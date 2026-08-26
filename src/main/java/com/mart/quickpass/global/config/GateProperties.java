package com.mart.quickpass.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gate")
public record GateProperties(String serviceSecret) {
}
