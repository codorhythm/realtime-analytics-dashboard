package com.analytics.service;

import com.analytics.domain.Order;
import com.analytics.domain.OrderEvent;
import com.analytics.domain.OrderStatus;
import com.analytics.kafka.OrderEventProducer;
import com.analytics.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderEventProducer producer;
    private final OrderRepository orderRepository;

    public void placeOrder(OrderEvent event) {
        event.setEventTime(LocalDateTime.now());
        producer.publishOrderEvent(event);
        log.info("Order event sent to Kafka: {}", event.getOrderId());
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Double getTotalRevenue(LocalDateTime start, LocalDateTime end) {
        return orderRepository.getTotalRevenue(start, end);
    }

    public List<Object[]> getOrderCountByStatus() {
        return orderRepository.getOrderCountByStatus();
    }

    public List<Object[]> getRevenueByRegion() {
        return orderRepository.getRevenueByRegion();
    }

    public List<Object[]> getOrdersPerMinute(int lastMinutes) {
        return orderRepository.getOrdersPerMinute(
                LocalDateTime.now().minusMinutes(lastMinutes)
        );
    }
}