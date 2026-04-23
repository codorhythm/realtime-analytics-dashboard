package com.analytics.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.orders}")
    private String ordersTopic;

    @Value("${app.kafka.topic.metrics}")
    private String metricsTopic;

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ordersTopic)
                .partitions(1)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic apiMetricsTopic() {
        return TopicBuilder.name(metricsTopic)
                .partitions(1)
                .replicas(2)
                .build();
    }
}