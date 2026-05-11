package com.smartworks.smartworks_api.dto;

import java.util.List;

public class MonthlyAnalyticsResponseV2 {

    public int year;
    public int month;

    public long ordersThisMonth;
    public long ordersLastMonth;
    public double ordersChangePct;

    public long pendingThisMonth;
    public long completedThisMonth;
    public long cancelledThisMonth;

    public double avgDeliveryHoursThisMonth;
    public double avgDeliveryChangePct;

    public List<TopProduct> topProductsByUnits;

    public double revenueThisMonth;
    public double revenueLastMonth;
    public double revenueChangePct;

    public long unitsSoldThisMonth;
    public double avgOrderValueThisMonth;

    public static class TopProduct {
        public Long productId;
        public String name;
        public Long quantity;

        public TopProduct(Long productId, String name, Long quantity) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
        }
    }
}