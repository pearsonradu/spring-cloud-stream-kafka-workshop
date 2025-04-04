package com.example.demo;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * {@see <a href=
 * "https://docs.spring.io/spring-kafka/docs/2.7.x/reference/html/#message-listener-container">
 * Message Listener Container</a>}
 */
class NullValueFilterBatchInterceptor<K, V> implements BatchInterceptor<K, V> {

    @Override
    @Nullable
    public ConsumerRecords<K, V> intercept(@NonNull ConsumerRecords<K, V> records, @NonNull Consumer<K, V> consumer) {
        return records.partitions().stream()
                .map(partition -> StreamSupport.stream(records.spliterator(), false)
                        .filter(consumerRecord -> consumerRecord.partition() == partition.partition())
                        .filter(consumerRecord -> {
                            var hasValue = consumerRecord.value() != null;

                            if (!hasValue) {
                                System.out.println("Dropping Kafka consumer record with topic " + partition.topic()
                                        + " partition " + consumerRecord.partition()
                                        + " offset " + consumerRecord.offset()
                                        + " due to null value");
                            }

                            return hasValue;
                        })
                        .collect(Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> Map.entry(partition, list))))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue),
                        ConsumerRecords::new));
    }

}
