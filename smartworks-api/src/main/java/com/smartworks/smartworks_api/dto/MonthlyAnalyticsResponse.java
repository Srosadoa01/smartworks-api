package com.smartworks.smartworks_api.dto;

import java.util.List;

public class MonthlyAnalyticsResponse {
    public int year;
    public int month;

    public long ordersThisMonth;
    public long ordersLastMonth;
    public double ordersChangePct;

    public long pendingThisMonth;
    public long completedThisMonth;

    public double avgDeliveryHoursThisMonth; // tiempo medio entrega
    public double avgDeliveryChangePct;

    public List<TopProduct> topProducts;

    public static class TopProduct {
        public Long productId;
        public String name;
        public long quantity;

        public TopProduct(Long productId, String name, long quantity) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
        }
    }
}
