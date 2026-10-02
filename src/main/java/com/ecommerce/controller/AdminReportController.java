package com.ecommerce.controller;

import com.ecommerce.dto.SalesReportResponse;
import com.ecommerce.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public SalesReportResponse summary(@RequestParam(defaultValue = "5") int lowStockThreshold) {
        return reportService.summary(lowStockThreshold);
    }
}