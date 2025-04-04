package com.example.demo;

import java.util.List;
import java.util.function.Function;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * {@see <a href=
 * "https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#_batch_consumers">
 * Spring Cloud Stream binder batch consumer reference
 * </a>}
 * <br/>
 * <br/>
 * {@see <a href=
 * "https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#_batch_producers">
 * Spring Cloud Stream binder batch producer reference
 * </a>}
 */
class EventProcessor implements Function<Message<List<Event>>, List<Message<Event>>> {

    @Override
    public List<Message<Event>> apply(Message<List<Event>> message) {
        System.out.println("Processor event: Start");

        message.getPayload().forEach(event -> System.out.println("Processor event: " + event));

        System.out.println("Processor event: End");

        return message.getPayload()
                .stream()
                .map(event -> MessageBuilder.withPayload(event).build())
                .toList();
    }

}
