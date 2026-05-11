package com.smartworks.smartworks_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartworks.smartworks_api.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
