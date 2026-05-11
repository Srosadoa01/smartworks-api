package com.smartworks.smartworks_api.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.smartworks.smartworks_api.dto.CreateOrderRequest;
import com.smartworks.smartworks_api.entity.Product;
import com.smartworks.smartworks_api.repository.ProductRepository;
import com.smartworks.smartworks_api.service.OrderService;

@Component
public class ToolExecutor {

    private final ProductRepository productRepo;
    private final OrderService orderService;

    public ToolExecutor(ProductRepository productRepo, OrderService orderService) {
        this.productRepo = productRepo;
        this.orderService = orderService;
    }

    // tool: get_low_stock_products
    public Object getLowStockProducts() {
        List<Product> list = productRepo.findLowStock();
        return list.stream().map(p -> Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "stock", p.getStock(),
                "lowStockThreshold", p.getLowStockThreshold(),
                "price", p.getPrice()
        )).collect(Collectors.toList());
    }

    // tool: create_order
    public Object createOrder(Map<String, Object> args) {
        // args: { customerId: number, lines: [{productId:number, quantity:number}] }
        Number customerIdNum = (Number) args.get("customerId");
        if (customerIdNum == null) throw new IllegalArgumentException("customerId requerido");
        long customerId = customerIdNum.longValue();

        List<Map<String, Object>> linesRaw = (List<Map<String, Object>>) args.get("lines");
        if (linesRaw == null || linesRaw.isEmpty()) throw new IllegalArgumentException("lines requerido");

        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerId(customerId);

        List<CreateOrderRequest.Line> lines = new ArrayList<>();
        for (Map<String, Object> l : linesRaw) {
            Number pid = (Number) l.get("productId");
            Number qty = (Number) l.get("quantity");
            if (pid == null || qty == null) continue;

            CreateOrderRequest.Line line = new CreateOrderRequest.Line();
            line.setProductId(pid.longValue());
            line.setQuantity(qty.intValue());
            lines.add(line);
        }

        req.setLines(lines);

        var created = orderService.create(req);

        return Map.of(
                "orderId", created.getId(),
                "status", created.getStatus().name(),
                "createdAt", created.getCreatedAt().toString(),
                "customerId", created.getCustomer().getId(),
                "linesCount", created.getLines() == null ? 0 : created.getLines().size()
        );
    }
}
