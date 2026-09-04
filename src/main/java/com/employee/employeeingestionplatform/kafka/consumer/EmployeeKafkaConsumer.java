package com.employee.employeeingestionplatform.kafka.consumer;

import com.employee.employeeingestionplatform.dto.kafka.EmployeeKafkaEvent;
import com.employee.employeeingestionplatform.service.KafkaEmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class EmployeeKafkaConsumer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmployeeKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final KafkaEmployeeService kafkaEmployeeService;

    public EmployeeKafkaConsumer(
            ObjectMapper objectMapper,
            KafkaEmployeeService kafkaEmployeeService
    ) {
        this.objectMapper = objectMapper;
        this.kafkaEmployeeService = kafkaEmployeeService;
    }

    @KafkaListener(
            topics = "${app.kafka.employee-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {
        EmployeeKafkaEvent event = parse(payload);

        LOGGER.info(
                "Received Kafka employee event for {}",
                event.empId()
        );

        kafkaEmployeeService.ingest(event);

        LOGGER.info(
                "Successfully ingested Kafka employee {}",
                event.empId()
        );
    }

    private EmployeeKafkaEvent parse(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    EmployeeKafkaEvent.class
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid employee event JSON",
                    exception
            );
        }
    }
}