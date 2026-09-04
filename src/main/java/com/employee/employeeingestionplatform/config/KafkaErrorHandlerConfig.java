package com.employee.employeeingestionplatform.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.kafka.dead-letter-topic}")
            String deadLetterTopic
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        deadLetterTopic,
                                        record.partition()
                                )
                );

        FixedBackOff backOff = new FixedBackOff(
                1000L,
                2L
        );

        return new DefaultErrorHandler(recoverer, backOff);
    }
}