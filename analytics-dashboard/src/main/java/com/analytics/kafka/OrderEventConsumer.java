package com.analytics.kafka;

import com.analytics.domain.Order;
import com.analytics.domain.OrderEvent;
import com.analytics.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${app.kafka.topic.orders}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeOrderEvent(OrderEvent event) {
        log.info("Received order event: orderId={}, status={}",
                event.getOrderId(), event.getStatus());

        try {
            Order order = Order.builder()
                    .orderId(event.getOrderId())
                    .customerId(event.getCustomerId())
                    .productId(event.getProductId())
                    .productName(event.getProductName())
                    .amount(event.getAmount())
                    .quantity(event.getQuantity())
                    .status(event.getStatus())
                    .region(event.getRegion())
                    .eventTime(event.getEventTime())
                    .build();

            orderRepository.save(order);
            log.info("Order saved to PostgreSQL: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order event: orderId={}, error={}",
                    event.getOrderId(), e.getMessage());
        }
    }
}