package com.smartworks.smartworks_api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartworks.smartworks_api.dto.CreateMovementRequest;
import com.smartworks.smartworks_api.entity.Product;
import com.smartworks.smartworks_api.entity.StockMovement;
import com.smartworks.smartworks_api.repository.ProductRepository;
import com.smartworks.smartworks_api.repository.StockMovementRepository;

@Service
public class StockMovementService {

    private final ProductRepository productRepo;
    private final StockMovementRepository movementRepo;

    public StockMovementService(ProductRepository productRepo, StockMovementRepository movementRepo) {
        this.productRepo = productRepo;
        this.movementRepo = movementRepo;
    }

    @Transactional
    public StockMovement create(CreateMovementRequest req) {
        if (req.productId == null)
            throw new IllegalArgumentException("productId is required");
        if (req.type == null)
            throw new IllegalArgumentException("type is required");
        if (req.quantity <= 0)
            throw new IllegalArgumentException("quantity must be > 0");

        Product p = productRepo.findById(req.productId)
                .orElseThrow(() -> new IllegalArgumentException("product not found"));

        int newStock = p.getStock();

        switch (req.type) {
            case IN -> newStock += req.quantity;
            case OUT -> newStock -= req.quantity;
        }

        if (newStock < 0) {
            throw new IllegalStateException("stock cannot be negative");
        }

        p.setStock(newStock);
        productRepo.save(p);

        StockMovement m = new StockMovement();
        m.setProduct(p);
        m.setType(req.type);
        m.setQuantity(req.quantity);
        m.setNote(req.note);

        return movementRepo.save(m);
    }
}