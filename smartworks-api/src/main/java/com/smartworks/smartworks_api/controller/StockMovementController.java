package com.smartworks.smartworks_api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.dto.CreateMovementRequest;
import com.smartworks.smartworks_api.entity.StockMovement;
import com.smartworks.smartworks_api.repository.StockMovementRepository;
import com.smartworks.smartworks_api.service.StockMovementService;

@RestController
@RequestMapping("/movements")
public class StockMovementController {

    private final StockMovementRepository repo;
    private final StockMovementService service;

    public StockMovementController(StockMovementRepository repo, StockMovementService service) {
        this.repo = repo;
        this.service = service;
    }

    @GetMapping
    public List<StockMovement> list() {
        return repo.findAll();
    }

    @PostMapping
    public StockMovement create(@RequestBody CreateMovementRequest req) {
        return service.create(req);
    }
}
