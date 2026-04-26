package com.analytics.service;

import com.analytics.domain.OrderEvent;
import com.analytics.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class OrderSimulatorService {

    private final OrderService orderService;


    private static final List<String> PRODUCT_IDS = List.of(
            "PROD-001", "PROD-002", "PROD-003", "PROD-004", "PROD-005", "PROD-006"
    );
    private static final List<String> PRODUCT_NAMES = List.of(
            "Wireless Headphones", "Mechanical Keyboard", "USB-C Hub",
            "4K Monitor", "Ergonomic Chair", "Standing Desk"
    );
    private static final List<Double> PRODUCT_PRICES = List.of(
            99.99, 149.99, 39.99, 449.99, 299.99, 599.99
    );

    private static final List<String> REGIONS = List.of(
            "APAC", "APAC", "APAC", "EMEA", "EMEA", "AMER", "AMER", "AMER", "AMER"
    );

    @Scheduled(fixedDelayString = "${app.simulator.interval-ms:2000}")
    public void generateOrder() {
        int productIdx = ThreadLocalRandom.current().nextInt(PRODUCT_IDS.size());
        int quantity = ThreadLocalRandom.current().nextInt(1, 5);

        OrderEvent event = OrderEvent.builder()
                .orderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customerId("CUST-" + ThreadLocalRandom.current().nextInt(1, 200))
                .productId(PRODUCT_IDS.get(productIdx))
                .productName(PRODUCT_NAMES.get(productIdx))
                .amount(PRODUCT_PRICES.get(productIdx) * quantity)
                .quantity(quantity)
                .status(randomStatus())
                .region(REGIONS.get(ThreadLocalRandom.current().nextInt(REGIONS.size())))
                .eventTime(LocalDateTime.now())
                .build();

        orderService.placeOrder(event);
        log.debug("Simulator generated order: {}", event.getOrderId());
    }

    private OrderStatus randomStatus() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 50) return OrderStatus.PLACED;
        if (roll < 70) return OrderStatus.CONFIRMED;
        if (roll < 85) return OrderStatus.SHIPPED;
        if (roll < 95) return OrderStatus.DELIVERED;
        if (roll < 98) return OrderStatus.CANCELLED;
        return OrderStatus.PAYMENT_FAILED;
    }
}