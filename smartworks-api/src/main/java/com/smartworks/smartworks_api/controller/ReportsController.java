package com.smartworks.smartworks_api.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.entity.GeneratedReport;
import com.smartworks.smartworks_api.service.ReportService;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    // POST /reports/monthly?year=2026&month=3
    @PostMapping("/monthly")
    public Map<String, Object> generateMonthly(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        GeneratedReport rep = reportService.generateAndSaveMonthlyReport(year, month);

        // IMPORTANTE: NO devuelvas el pdfBytes aquí (pesa muchísimo)
        return Map.of(
                "id", rep.getId(),
                "year", rep.getYear(),
                "month", rep.getMonth(),
                "createdAt", rep.getCreatedAt(),
                "filename", rep.getFilename());
    }

    // GET /reports/{id}/download
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        GeneratedReport rep = reportService.getOrThrow(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + rep.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(rep.getPdfBytes());
    }
}
