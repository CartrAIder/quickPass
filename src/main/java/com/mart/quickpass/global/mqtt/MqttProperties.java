package com.mart.quickpass.global.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

// yml 파일의 설정값을 가져온다
@ConfigurationProperties(prefix = "mqtt")
public record MqttProperties(
        String brokerUrl,   // 브로커 접속 URL (tcp://host:port)
        String clientId,    // 서버 클라이언트 ID
        String scanTopic,   // 구독할 스캔 토픽 (와일드카드 가능)
        int qos             // 구독 QoS (0/1/2)
) {
}
