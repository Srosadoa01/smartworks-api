package com.smartworks.smartworks_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.dto.MonthlyAnalyticsResponse;
import com.smartworks.smartworks_api.service.AnalyticsService;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/monthly")
    public MonthlyAnalyticsResponse monthly(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return analyticsService.monthly(year, month);
    }
}
