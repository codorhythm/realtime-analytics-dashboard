package com.analytics.repository;

import com.analytics.domain.Order;
import com.analytics.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.eventTime BETWEEN :start AND :end")
    Double getTotalRevenue(LocalDateTime start, LocalDateTime end);


    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderCountByStatus();


    @Query(value = """
        SELECT DATE_TRUNC('minute', event_time) as minute,
               COUNT(*) as order_count,
               SUM(amount) as revenue
        FROM orders
        WHERE event_time >= :since
        GROUP BY DATE_TRUNC('minute', event_time)
        ORDER BY minute
        """, nativeQuery = true)
    List<Object[]> getOrdersPerMinute(LocalDateTime since);


    @Query("SELECT o.region, SUM(o.amount) FROM Order o GROUP BY o.region")
    List<Object[]> getRevenueByRegion();

    List<Order> findByStatus(OrderStatus status);
}