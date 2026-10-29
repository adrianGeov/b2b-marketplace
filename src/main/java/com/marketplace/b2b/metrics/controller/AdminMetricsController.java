package com.marketplace.b2b.metrics.controller;

import com.marketplace.b2b.metrics.dto.MetricsResponse;
import com.marketplace.b2b.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final MetricsService metricsService;

    /** Ejemplo: /api/v1/admin/metrics?from=2026-08-01&to=2026-10-27 */
    @GetMapping
    public MetricsResponse metrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return metricsService.getMetrics(from, to);
    }
}