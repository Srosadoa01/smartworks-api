package com.smartworks.smartworks_api.dto;

import com.smartworks.smartworks_api.entity.StockMovement;

public class CreateMovementRequest {
    public Long productId;
    public StockMovement.Type type; // IN / OUT
    public int quantity;
    public String note;
}
