package com.smartworks.smartworks_api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartworks.smartworks_api.dto.CreateOrderRequest;
import com.smartworks.smartworks_api.entity.Customer;
import com.smartworks.smartworks_api.entity.Order;
import com.smartworks.smartworks_api.entity.OrderLine;
import com.smartworks.smartworks_api.entity.Product;
import com.smartworks.smartworks_api.repository.CustomerRepository;
import com.smartworks.smartworks_api.repository.OrderRepository;
import com.smartworks.smartworks_api.repository.ProductRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;

    public OrderService(OrderRepository orderRepo,
            CustomerRepository customerRepo,
            ProductRepository productRepo) {
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    @Transactional
    public Order create(CreateOrderRequest request) {

        if (request == null)
            throw new IllegalArgumentException("Request body is required");
        if (request.getCustomerId() == null)
            throw new IllegalArgumentException("customerId is required");
        if (request.getLines() == null || request.getLines().isEmpty())
            throw new IllegalArgumentException("lines is required and cannot be empty");

        Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);

        List<OrderLine> lines = new ArrayList<>();

        for (CreateOrderRequest.Line item : request.getLines()) {

            if (item.getProductId() == null)
                throw new IllegalArgumentException("productId is required");
            if (item.getQuantity() <= 0)
                throw new IllegalArgumentException("Quantity must be > 0");

            Product product = productRepo.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));

            if (product.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Not enough stock for product: " + product.getName());
            }

            // Restar stock
            product.setStock(product.getStock() - item.getQuantity());

            OrderLine line = new OrderLine();
            line.setOrder(order); // ✅ CLAVE
            line.setProduct(product);
            line.setQuantity(item.getQuantity());

            lines.add(line);
        }

        order.setLines(lines);

        return orderRepo.save(order);
    }
}