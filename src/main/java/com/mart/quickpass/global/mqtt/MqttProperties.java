package com.mart.quickpass.global.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// yml 파일의 설정값을 가져온다
@ConfigurationProperties(prefix = "mqtt")
public record MqttProperties(
        String brokerUrl,   // 브로커 접속 URL (ssl://host:port)
        String clientId,    // 서버 클라이언트 ID
        String username,    // MQTT 브로커 인증 사용자명
        String password,    // MQTT 브로커 인증 비밀번호
        String trustStorePath,      // TLS 서버 인증서 검증용 truststore 경로 (선택)
        String trustStorePassword,  // TLS truststore 비밀번호 (선택)
        String scanTopic,   // 구독할 스캔 토픽 (와일드카드 가능)
        int qos,            // 구독 QoS (0/1/2)
        Duration scanDeduplicationTtl // 같은 scanId의 중복 처리를 막는 Redis TTL
) {
}
