package com.analytics.kafka;

import com.analytics.domain.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.kafka.topic.orders}")
    private String ordersTopic;

    public void publishOrderEvent(OrderEvent event) {
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(ordersTopic, event.getOrderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Order event published: orderId={}, status={}, offset={}",
                        event.getOrderId(),
                        event.getStatus(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish order event: orderId={}, error={}",
                        event.getOrderId(), ex.getMessage());
            }
        });
    }
}