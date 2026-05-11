package com.smartworks.smartworks_api.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.dto.CreateOrderRequest;
import com.smartworks.smartworks_api.entity.Customer;
import com.smartworks.smartworks_api.entity.Order;
import com.smartworks.smartworks_api.entity.OrderLine;
import com.smartworks.smartworks_api.entity.Product;
import com.smartworks.smartworks_api.repository.CustomerRepository;
import com.smartworks.smartworks_api.repository.OrderLineRepository;
import com.smartworks.smartworks_api.repository.OrderRepository;
import com.smartworks.smartworks_api.repository.ProductRepository;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepo;
    private final OrderLineRepository orderLineRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;

    public OrderController(
            OrderRepository orderRepo,
            OrderLineRepository orderLineRepo,
            CustomerRepository customerRepo,
            ProductRepository productRepo
    ) {
        this.orderRepo = orderRepo;
        this.orderLineRepo = orderLineRepo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    @GetMapping
    public List<Order> list() {
        return orderRepo.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody CreateOrderRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Order request is required");
        }

        if (req.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer is required");
        }

        if (req.getLines() == null || req.getLines().isEmpty()) {
            throw new IllegalArgumentException("Order lines are required");
        }

        Customer customer = customerRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Customer not found: " + req.getCustomerId()
                ));

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(Order.Status.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setCompletedAt(null);

        List<OrderLine> orderLines = new ArrayList<>();

        for (CreateOrderRequest.Line lineReq : req.getLines()) {
            if (lineReq.getProductId() == null) {
                throw new IllegalArgumentException("Product is required");
            }

            if (lineReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }

            Product product = productRepo.findById(lineReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: " + lineReq.getProductId()
                    ));

            if (product.getStock() < lineReq.getQuantity()) {
                throw new NotEnoughStockException(
                        "Not enough stock for product: " + product.getName()
                );
            }

            product.setStock(product.getStock() - lineReq.getQuantity());
            productRepo.save(product);

            OrderLine orderLine = new OrderLine();
            orderLine.setOrder(order);
            orderLine.setProduct(product);
            orderLine.setQuantity(lineReq.getQuantity());

            orderLines.add(orderLine);
        }

        Order savedOrder = orderRepo.save(order);

        for (OrderLine line : orderLines) {
            line.setOrder(savedOrder);
        }

        orderLineRepo.saveAll(orderLines);

        savedOrder.setLines(orderLines);

        return orderRepo.save(savedOrder);
    }

    @PatchMapping("/{id}/status")
    public Order updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + id
                ));

        String statusText = body.get("status");

        if (statusText == null || statusText.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        Order.Status newStatus;

        try {
            newStatus = Order.Status.valueOf(statusText);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + statusText);
        }

        Order.Status oldStatus = order.getStatus();

        if (oldStatus == newStatus) {
            return order;
        }

        if (newStatus == Order.Status.CANCELLED && oldStatus != Order.Status.CANCELLED) {
            restoreStock(order);
            order.setCompletedAt(null);
        }

        if (oldStatus == Order.Status.CANCELLED && newStatus != Order.Status.CANCELLED) {
            discountStockAgain(order);
        }

        if (newStatus == Order.Status.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        if (newStatus != Order.Status.COMPLETED) {
            order.setCompletedAt(null);
        }

        order.setStatus(newStatus);

        return orderRepo.save(order);
    }

    private void restoreStock(Order order) {
        List<OrderLine> lines = getOrderLines(order);

        for (OrderLine line : lines) {
            Product product = line.getProduct();

            product.setStock(product.getStock() + line.getQuantity());

            productRepo.save(product);
        }
    }

    private void discountStockAgain(Order order) {
        List<OrderLine> lines = getOrderLines(order);

        for (OrderLine line : lines) {
            Product product = line.getProduct();

            if (product.getStock() < line.getQuantity()) {
                throw new NotEnoughStockException(
                        "Not enough stock for product: " + product.getName()
                );
            }
        }

        for (OrderLine line : lines) {
            Product product = line.getProduct();

            product.setStock(product.getStock() - line.getQuantity());

            productRepo.save(product);
        }
    }

    private List<OrderLine> getOrderLines(Order order) {
        if (order.getLines() != null && !order.getLines().isEmpty()) {
            return order.getLines();
        }

        return orderLineRepo.findByOrderId(order.getId());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    private static class NotEnoughStockException extends RuntimeException {
        public NotEnoughStockException(String message) {
            super(message);
        }
    }
}