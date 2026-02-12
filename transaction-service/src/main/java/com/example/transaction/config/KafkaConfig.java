package com.example.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.deposit-requested}")
    private String depositRequestedTopic;

    @Value("${app.kafka.topics.deposit-completed}")
    private String depositCompletedTopic;

    @Value("${app.kafka.topics.withdrawal-requested}")
    private String withdrawalRequestedTopic;

    @Value("${app.kafka.topics.withdrawal-completed}")
    private String withdrawalCompletedTopic;

    @Value("${app.kafka.topics.withdrawal-failed}")
    private String withdrawalFailedTopic;

    @Bean
    public NewTopic depositRequestedTopic() {
        return TopicBuilder.name(depositRequestedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic depositCompletedTopic() {
        return TopicBuilder.name(depositCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic withdrawalRequestedTopic() {
        return TopicBuilder.name(withdrawalRequestedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic withdrawalCompletedTopic() {
        return TopicBuilder.name(withdrawalCompletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic withdrawalFailedTopic() {
        return TopicBuilder.name(withdrawalFailedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}