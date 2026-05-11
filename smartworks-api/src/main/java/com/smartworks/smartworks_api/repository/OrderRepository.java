package com.smartworks.smartworks_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smartworks.smartworks_api.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByStatus(Order.Status status);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusAndCreatedAtBetween(Order.Status status, LocalDateTime from, LocalDateTime to);

    List<Order> findByStatusAndCompletedAtIsNotNullAndCreatedAtBetween(
            Order.Status status, LocalDateTime from, LocalDateTime to);

    List<Order> findByStatus(Order.Status status);

    @Query("select o from Order o where o.createdAt between :from and :to")
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<Order> findTop5ByOrderByCreatedAtDesc();
}