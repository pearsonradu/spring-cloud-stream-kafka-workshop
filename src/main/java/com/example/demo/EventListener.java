package com.example.demo;

import java.util.function.Consumer;

/**
 * {@see <a href=
 * "https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#spring-cloud-stream-preface-adding-message-handler">
 * Spring Cloud Stream consumer example reference
 * </a>}
 * <br/>
 * <br/>
 * {@see <a href=
 * "https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#_bindings">
 * Spring Cloud Stream binder reference
 * </a>}
 */
class EventListener implements Consumer<Event> {

    @Override
    public void accept(Event event) {
        System.out.println("Listener event: " + event);
    }

}
