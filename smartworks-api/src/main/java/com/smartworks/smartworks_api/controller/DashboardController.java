package com.smartworks.smartworks_api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.entity.Order;
import com.smartworks.smartworks_api.repository.OrderRepository;
import com.smartworks.smartworks_api.repository.ProductRepository;

@RestController
public class DashboardController {

    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;

    public DashboardController(ProductRepository productRepo,
            OrderRepository orderRepo) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> stats() {

        long lowStockProducts = productRepo.findAll()
                .stream()
                .filter(p -> p.getStock() <= p.getLowStockThreshold())
                .count();

        long pendingOrders = orderRepo.countByStatus(Order.Status.PENDING);

        return Map.of(
                "lowStockProducts", lowStockProducts,
                "pendingOrders", pendingOrders);
    }
}
