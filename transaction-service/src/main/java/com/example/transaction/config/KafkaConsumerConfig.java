package com.example.transaction.config;

import com.example.transaction.kafka.event.DepositCompletedEvent;
import com.example.transaction.kafka.event.WithdrawalCompletedEvent;
import com.example.transaction.kafka.event.WithdrawalFailedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private Map<String, Object> baseConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    private <T> ConsumerFactory<String, T> createConsumerFactory(Class<T> targetType) {
        Map<String, Object> props = baseConsumerConfigs();
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetType);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.example.transaction.kafka.event");
        deserializer.setUseTypeMapperForKey(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DepositCompletedEvent> depositCompletedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DepositCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createConsumerFactory(DepositCompletedEvent.class));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WithdrawalCompletedEvent> withdrawalCompletedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WithdrawalCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createConsumerFactory(WithdrawalCompletedEvent.class));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WithdrawalFailedEvent> withdrawalFailedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WithdrawalFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createConsumerFactory(WithdrawalFailedEvent.class));
        return factory;
    }
}
