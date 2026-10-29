package com.marketplace.b2b.metrics.controller;

import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.metrics.dto.MetricsResponse;
import com.marketplace.b2b.metrics.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = OpenApiConfig.TAG_ADMIN_METRICS)
public class AdminMetricsController {

    private final MetricsService metricsService;

    @GetMapping
    @Operation(summary = "Indicadores de negocio",
            description = "Ventas, ticket promedio, conversión de leads y de checkout, entregas a tiempo vs. "
                    + "tardías, productos más vendidos y combinaciones más frecuentes. "
                    + "Por defecto: últimos 90 días.")
    public MetricsResponse metrics(
            @Parameter(description = "Fecha inicial (inclusive)", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final (inclusive)", example = "2026-10-27")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return metricsService.getMetrics(from, to);
    }
}