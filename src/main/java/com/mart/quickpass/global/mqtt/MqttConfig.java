package com.mart.quickpass.global.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

// MQTT 수신 인프라 설정
@Configuration
public class MqttConfig {

    private final MqttProperties properties;

    // 생성자
    public MqttConfig(MqttProperties properties) {
        this.properties = properties;
    }

    // 브로커 연결 설정
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.brokerUrl()});
        options.setCleanSession(true);        // 서버는 세션을 브로커에 보존하지 않음
        options.setAutomaticReconnect(true);  // 연결 끊기면 자동 재연결
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setUserName(properties.username());
        options.setPassword(properties.password().toCharArray());
        factory.setConnectionOptions(options);
        return factory;
    }

    // 브로커에게 받은 메세지를 스프링 비즈니스 로직으로 넘김
    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    // 메세지 수신 어뎁터 설정
    @Bean
    public MessageProducer mqttInbound(MqttPahoClientFactory mqttClientFactory,
                                       MessageChannel mqttInboundChannel) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        properties.clientId(),
                        mqttClientFactory,
                        properties.scanTopic());

        adapter.setCompletionTimeout(5000); // 타임아웃(5초)
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(properties.qos());
        adapter.setOutputChannel(mqttInboundChannel);
        return adapter;
    }
}
