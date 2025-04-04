# Spring Cloud Stream Kafka Workshop

## 🥅 Goal

Develop familiarity with Kafka and Spring Cloud Stream to promote understanding and best practices.

## ✏️ Prerequisites

The following is required to work utilize this project:

- [Java 21](https://adoptium.net/temurin/releases/?version=21)
- [Docker & Docker Compose](https://docs.docker.com/)
- [Kaf](https://github.com/birdayz/kaf)

## 📘 Steps

### Understanding the Project Layout

Get to know the project! A Maven wrapper is configured for a single module application. Dependencies have been added for Spring Boot and Spring Cloud. There is a base package called `com.example.demo` that is ready for Java files!

### Starting Kafka with Docker Compose

This workshop will use Kafka to produce and consume messages. There is a [Kafka Docker Compose](./docker-compose/kafka/docker-compose.yaml) file which configures Zookeeper, Kafka and Kafdrop.

To start the containers run the following command:

```shell
docker compose -f docker-compose/kafka/docker-compose.yaml up -d
```

### Visualizing Kafka Details with Kafdrop

Navigate to [http://localhost:9000](http://localhost:9000) to see details, like topics and messages, throughout the workshop with [Kafdrop](https://github.com/obsidiandynamics/kafdrop).

### Create an Java POJO to send to Kafka as JSON

A JSON message can be sent over Kafka to provide helpful details to a consumer. Create a Java `record` in the `com.example.demo` package called `Event` that has a `message` field. This class will act as the data transfer object (DTO).

### Create a Spring Cloud Stream Consumer

Scenario: We want our application to consume messages from Kafka so we can trigger an action.

Define a Spring Cloud Stream consumer that accepts `Event` messages and logs them:

1. Create a class called `EventListener`
   1. Make `EventListener` implement `java.util.function.Consumer` for an `Event`
   2. The listener should log the message object
2. Create a class called `StreamConfiguration`
   1. Annotate the class with `@Configuration`
   2. Create a `@Bean` definition called `eventListener` for `java.util.function.Consumer<Event>` that initializes an `EventListener`
3. In the `application.yaml` add the following properties
   1. Configure the Spring Cloud Stream function by setting `spring.cloud.function.definition=eventListener`
   2. Configure a new Spring Cloud Stream binding by setting `spring.cloud.stream.bindings.eventListener-in-0` with the following properties

      1. ```yaml
          group: group-name
          destination: event-output
          consumer:
            max-attempts: 1
            use-native-decoding: true
         ```

   3. Configure the Spring Cloud Stream Kafka binder broker address by setting `spring.cloud.stream.kafka.binder.brokers=localhost:9092`
   4. Configure a Spring Cloud Stream Kafka binding for `spring.cloud.stream.kafka.bindings.eventListener-in-0`

        1. ```yaml
            consumer:
              configuration:
                value.deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
                spring.json.trusted.packages: com.example.demo
           ```

#### Test the Event Listener

1. Start the Spring Boot application
2. Use `kaf` to publish a message `echo '{ "message": "hello" }' | kaf produce event-output --header '__TypeId__:com.example.demo.Event'`
3. The application should log the message from `EventListener`

### Create a Spring Cloud Stream Function

Scenario: We want to consume a batch of messages from Kafka so we can run an action and publish success events.

Define a Spring Cloud Stream function that consumes a batch of `Event` messages, logs them, and publishes them:

1. Create a class called `EventProcessor`
   1. Make `EventProcessor` implement `java.util.function.Function` for a `Message<List<Event>>` and `List<Message<Event>>`
   2. Log each event message
2. Modify the `StreamConfiguration` class to create a `@Bean` definition called `eventProcessor` for `java.util.function.Function<Message<List<Event>>, List<Message<Event>>>` that initializes an `EventProcessor`
3. In the `application.yaml` add the following properties
   1. Configure the Spring Cloud Stream function by setting `spring.cloud.function.definition=eventProcessor;eventListener`
   2. Configure a new Spring Cloud Stream binding by setting `spring.cloud.stream.bindings` with the following properties

      1. ```yaml
          eventProcessor-in-0:
            group: group-name
            destination: event-input
            consumer:
                max-attempts: 1
                use-native-decoding: true
          eventProcessor-out-0:
            destination: event-output
            producer:
              use-native-encoding: true
         ```

   3. Configure a Spring Cloud Stream Kafka binding for `eventListener-in-0`

        1. ```yaml
            eventProcessor-in-0:
              consumer:
                configuration:
                  value.deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
                  spring.json.trusted.packages: com.example.demo
            eventProcessor-out-0:
              producer:
                configuration:
                  value.serializer: org.springframework.kafka.support.serializer.JsonSerializer
           ```

#### Test the Event Processor

1. Start the Spring Boot application
2. Use `kaf` to publish a message `echo '{ "message": "hello" }' | kaf produce event-input --header '__TypeId__:com.example.demo.Event'`
3. The application should log the message from `EventProcessor`
4. The application should also log the message from `EventListener` since `EventProcessor` forwards the object

### Add DLQ Support for Spring Cloud Stream Function Failures

Scenario: We want to consume messages from Kafka and handle when the action fails so we can continue processing and send the message somewhere to be reviewed.

Modify the Spring Cloud Stream function so exceptions caused within the function are published to a DLQ topic:

1. Modify the `EventProcessor` to check if the `Event#message` property contains a `w` and if so throw a `RuntimeException`
2. In the `application.yaml` add the following properties to the Spring Cloud Stream Kafka binding `eventProcessor-in-0`

   1. ```yaml
        enable-dlq: true
        dlq-name: event-dlq
        dlq-producer-properties:
            configuration:
                key.serializer: org.apache.kafka.common.serialization.ByteArraySerializer
                value.serializer: org.springframework.kafka.support.serializer.JsonSerializer
      ```

#### Test the Event Processor DLQ

1. Start the Spring Boot application
2. Use `kaf` to publish a bad message `echo '{ "message": "wello" }' | kaf produce event-input --header '__TypeId__:com.example.demo.Event'`
3. The application should throw an exception in the `EventProcessor`
4. Use Kafdrop to inspect that the message was sent to the `event-dlq` topic

### Create a Spring Cloud Stream Consumer for Failed Messages

Scenario: We want to consume messages from Kafka and run actions on failed messages.

Define a Spring Cloud Stream consumer that accepts `Event` messages and logs them:

1. Create a class called `DlqEventListener`
   1. Make `DlqEventListener` implement `java.util.function.Consumer` for an `Event`
   2. The listener should log the message object
2. Modify `StreamConfiguration` to create a `@Bean` definition called `dlqEventListener` for `java.util.function.Consumer<Event>` that initializes an `DlqEventListener`
3. In the `application.yaml` add the following properties
   1. Configure the Spring Cloud Stream function by setting `spring.cloud.function.definition=eventProcessor;eventListener;dlqEventListener`
   2. Configure a new Spring Cloud Stream binding by setting `spring.cloud.stream.bindings.dlqEventListener-in-0` with the following properties

      1. ```yaml
          group: group-name
          destination: event-dlq
          consumer:
            max-attempts: 1
            use-native-decoding: true
         ```

   3. Configure a Spring Cloud Stream Kafka binding for `spring.cloud.stream.kafka.bindings.dlqEventListener-in-0`

        1. ```yaml
            consumer:
              configuration:
                value.deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
                spring.json.trusted.packages: com.example.demo
           ```

#### Test the DLQ Event Listener

1. Start the Spring Boot application
2. Use `kaf` to publish a bad message `echo '{ "message": "wello" }' | kaf produce event-input --header '__TypeId__:com.example.demo.Event'`
3. The application should throw an exception in the `EventProcessor`
4. The application should log the failure message from the `DlqEventListener`

### Add Batch Deserialization Error Handling

Scenario: We want to consume a batch of messages from Kafka and handle when one or many messages are in an invalid format so we can continue processing.

Modify the application to handle deserialization exceptions when a batch of messages are read by the `EventProcessor`:

1. Create a class called `NullValueFilterBatchInterceptor` that implements `org.springframework.kafka.listener.BatchInterceptor<K, V>`
   1. The `BatchInterceptor#intercept` method should be modified to ignore all Kafka consumer records with null values
   2. Null values are produced when consuming batch records and _x_ item(s) fail to deserialize. This results in a `KafkaNull` message being passed into `Message<List<Event>>` and a `ClassCastException` will be lazily thrown when `Message#getPayload` is called.
2. Modify `StreamConfiguration` to create a `@Bean` definition for `org.springframework.cloud.stream.config.ListenerContainerCustomizer<org.springframework.kafka.listener.AbstractMessageListenerContainer<?, ?>>`
   1. Configure the `container` parameter to set a batch intercept of `NullValueFilterBatchInterceptor`
3. In the `application.yaml` add the following properties to the Spring Cloud Stream Kafka binding `dlqEventListener-in-0`

   1. ```yaml
        value.deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
      ```

#### Test the Event Processor deserialization handling

1. Start the Spring Boot application
2. Use `kaf` to publish a bad message `echo ' "message": "wello" }' | kaf produce event-input --header '__TypeId__:com.example.demo.Event'`
3. The application should log the deserialization failure

### Stopping Kafka with Docker Compose

To stop the containers run the following command:

```shell
docker compose -f docker-compose/kafka/docker-compose.yaml down
```

## References

- [Spring Cloud Stream consumer example reference](https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#spring-cloud-stream-preface-adding-message-handler)
- [Spring Cloud Stream binder reference](https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#_bindings)
- [Spring Cloud Stream binder batch consumer reference](https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#_batch_consumers)
- [Spring Cloud Stream binder batch producer reference](https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream.html#_batch_producers)
- [Spring Cloud Stream Kafka simple DLQ](https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream-binder-kafka.html#_simple_dlq_with_kafka)
- [Spring Cloud Stream Kafka handling deserialization errors](https://docs.spring.io/spring-cloud-stream/docs/3.2.x/reference/html/spring-cloud-stream-binder-kafka.html#_handling_deserialization_errors_with_dlq)
- [Message listener container](https://docs.spring.io/spring-kafka/docs/2.7.x/reference/html/#message-listener-container)
