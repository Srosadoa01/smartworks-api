package com.smartworks.smartworks_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smartworks.smartworks_api.entity.OrderLine;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    List<OrderLine> findByOrderId(Long orderId);

    @Query("""
                select ol.product.id, ol.product.name, sum(ol.quantity)
                from OrderLine ol
                where ol.order.status = com.smartworks.smartworks_api.entity.Order.Status.COMPLETED
                and ol.order.createdAt between :from and :to
                group by ol.product.id, ol.product.name
                order by sum(ol.quantity) desc
            """)
    List<Object[]> topProducts(LocalDateTime from, LocalDateTime to);

    @Query("""
                select coalesce(sum(ol.quantity), 0)
                from OrderLine ol
                where ol.order.status = com.smartworks.smartworks_api.entity.Order.Status.COMPLETED
                and ol.order.createdAt between :from and :to
            """)
    Long totalUnits(LocalDateTime from, LocalDateTime to);

    @Query("""
                select coalesce(sum(ol.quantity * ol.product.price), 0)
                from OrderLine ol
                where ol.order.status = com.smartworks.smartworks_api.entity.Order.Status.COMPLETED
                and ol.order.createdAt between :from and :to
            """)
    Double revenue(LocalDateTime from, LocalDateTime to);
}