package com.example.demo;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StreamConfiguration {

    @Bean
    Consumer<Event> eventListener() {
        return new EventListener();
    }

}
