package com.analytics.controller;

import com.analytics.domain.OrderEvent;
import com.analytics.domain.OrderStatus;
import com.analytics.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> publishOrderEvent(
            @Valid @RequestBody OrderEvent event) {
        orderService.placeOrder(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "Order event published to Kafka",
                "orderId", event.getOrderId(),
                "status", event.getStatus(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping
    public ResponseEntity<List<?>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<?>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<Map<String, Object>> getTotalRevenue(
            @RequestParam(defaultValue = "24") int lastHours) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(lastHours);
        Double revenue = orderService.getTotalRevenue(start, end);
        return ResponseEntity.ok(Map.of(
                "totalRevenue", revenue,
                "lastHours", lastHours,
                "from", start.toString(),
                "to", end.toString()
        ));
    }

    @GetMapping("/analytics/by-status")
    public ResponseEntity<List<Object[]>> getOrdersByStatusCount() {
        return ResponseEntity.ok(orderService.getOrderCountByStatus());
    }

    @GetMapping("/analytics/by-region")
    public ResponseEntity<List<Object[]>> getRevenueByRegion() {
        return ResponseEntity.ok(orderService.getRevenueByRegion());
    }

    @GetMapping("/analytics/per-minute")
    public ResponseEntity<List<Object[]>> getOrdersPerMinute(
            @RequestParam(defaultValue = "30") int lastMinutes) {
        return ResponseEntity.ok(orderService.getOrdersPerMinute(lastMinutes));
    }
}