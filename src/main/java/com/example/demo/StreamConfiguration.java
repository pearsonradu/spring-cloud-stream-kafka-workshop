package com.example.demo;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}
