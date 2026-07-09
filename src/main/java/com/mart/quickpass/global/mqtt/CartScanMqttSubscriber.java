package com.mart.quickpass.global.mqtt;

import tools.jackson.databind.ObjectMapper;
import com.mart.quickpass.cart.dto.CartScanMessage;
import com.mart.quickpass.cart.service.CartScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class CartScanMqttSubscriber {

    private static final int CART_ID_TOPIC_INDEX = 2; // quickpass/cart/{cartId}/scan, 카트 번호가 2번째

    private final CartScanService cartScanService;
    private final ObjectMapper objectMapper;

    // 생성자
    public CartScanMqttSubscriber(CartScanService cartScanService, ObjectMapper objectMapper) {
        this.cartScanService = cartScanService;
        this.objectMapper = objectMapper;
    }

    // 수신 설정
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handle(Message<String> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC); // 토픽 확인
        String payload = message.getPayload();

        // 잘못된 메시지 하나가 수신 스레드를 죽이지 않도록 처리
        try {
            Long cartId = extractCartId(topic);
            CartScanMessage scan = objectMapper.readValue(payload, CartScanMessage.class);
            cartScanService.handleScan(cartId, scan);
        } catch (Exception e) {
            log.warn("[Mqtt] 스캔 메시지 처리 실패 - topic={}, payload={}", topic, payload, e);
        }
    }

    // cartId 추출 메서드
    private Long extractCartId(String topic) {
        if (topic == null) {
            throw new IllegalArgumentException("MQTT 토픽이 비어 있습니다");
        }
        String[] segments = topic.split("/");
        if (segments.length <= CART_ID_TOPIC_INDEX) {
            throw new IllegalArgumentException("예상과 다른 토픽 형식: " + topic);
        }
        return Long.parseLong(segments[CART_ID_TOPIC_INDEX]);
    }
}
