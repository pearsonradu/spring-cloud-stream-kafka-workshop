package com.example.demo;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.messaging.Message;

@Configuration
class StreamConfiguration {

    @Bean
    Function<Message<List<Event>>, List<Message<Event>>> eventProcessor() {
        return new EventProcessor();
    }

    @Bean
    Consumer<Event> eventListener() {
        return new EventListener();
    }

    @Bean
    Consumer<Event> dlqEventListener() {
        return new DlqEventListener();
    }

    /**
     * {@see <a href=
     * "https://docs.spring.io/spring-kafka/docs/2.7.x/reference/html/#message-listener-container">
     * Message Listener Container</a>}
     */
    @Bean
    ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer() {
        return (container, destinationName, group) -> {
            container.setBatchInterceptor(new NullValueFilterBatchInterceptor<>());
        };
    }

}
