package com.smartworks.smartworks_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.dto.MonthlyAnalyticsResponseV2;
import com.smartworks.smartworks_api.service.AnalyticsServiceV2;

@RestController
public class AnalyticsControllerV2 {

    private final AnalyticsServiceV2 service;

    public AnalyticsControllerV2(AnalyticsServiceV2 service) {
        this.service = service;
    }

    @GetMapping("/analytics/monthly-v2")
    public MonthlyAnalyticsResponseV2 monthly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return service.monthly(year, month);
    }
}