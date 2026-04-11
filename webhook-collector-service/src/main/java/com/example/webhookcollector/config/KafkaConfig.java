package com.example.webhookcollector.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.payment-status-updated}")
    private String paymentStatusUpdatedTopic;

    @Value("${kafka.topics.partitions:3}")
    private int partitions;

    @Value("${kafka.topics.replicas:1}")
    private short replicas;

    @Bean
    public NewTopic paymentStatusUpdatedTopic() {
        return TopicBuilder.name(paymentStatusUpdatedTopic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}