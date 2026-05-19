package com.platform.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(TopicNames.ORDER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(TopicNames.PAYMENT_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(TopicNames.PAYMENT_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationTriggeredTopic() {
        return TopicBuilder.name(TopicNames.NOTIFICATION_TRIGGERED).partitions(3).replicas(1).build();
    }
}
