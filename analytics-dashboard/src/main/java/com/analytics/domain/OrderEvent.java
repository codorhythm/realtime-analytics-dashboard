package com.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private String orderId;
    private String customerId;
    private String productId;
    private String productName;
    private Double amount;
    private Integer quantity;
    private OrderStatus status;
    private String region;
    private LocalDateTime eventTime;
}