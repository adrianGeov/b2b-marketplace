package com.marketplace.b2b.metrics.service;

import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.metrics.dto.MetricsResponse;
import com.marketplace.b2b.metrics.dto.MetricsResponse.Conversion;
import com.marketplace.b2b.metrics.dto.MetricsResponse.Deliveries;
import com.marketplace.b2b.metrics.dto.MetricsResponse.Period;
import com.marketplace.b2b.metrics.dto.MetricsResponse.Sales;
import com.marketplace.b2b.metrics.repository.MetricsRepository;
import com.marketplace.b2b.metrics.repository.MetricsRepository.ConversionCounts;
import com.marketplace.b2b.metrics.repository.MetricsRepository.DeliveryCounts;
import com.marketplace.b2b.metrics.repository.MetricsRepository.SalesSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricsService {

    static final int DEFAULT_RANGE_DAYS = 90;
    static final int MAX_RANGE_DAYS = 366;
    static final int TOP_LIMIT = 5;

    private static final List<String> NOTES = List.of(
            "checkoutConversionRate mide pedidos pagados vs. creados. El porcentaje de VISITANTES que compran "
                    + "requiere analítica web en el front (ej. GA4) para contar visitas.",
            "La rentabilidad por promoción requiere un módulo de promociones (fuera del MVP): "
                    + "tabla de promociones, promoción aplicada por pedido y costo del producto para calcular margen.");

    private final MetricsRepository repository;
    private final Clock clock;

    /**
     * @param from fecha inicial (inclusive); por defecto, 90 días antes de 'to'
     * @param to   fecha final (inclusive); por defecto, hoy
     */
    @Transactional(readOnly = true)
    public MetricsResponse getMetrics(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS);
        validateRange(start, end);

        OffsetDateTime fromTs = start.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC); // 'to' inclusive

        SalesSummary sales = repository.salesSummary(fromTs, toTs);
        ConversionCounts conversion = repository.conversionCounts(fromTs, toTs);
        DeliveryCounts deliveries = repository.deliveryCounts(fromTs, toTs);

        return new MetricsResponse(
                new Period(start, end),
                new Sales(sales.paidOrders(), money(sales.revenue()), money(sales.averageTicket())),
                new Conversion(conversion.leads(), conversion.buyers(), rate(conversion.buyers(), conversion.leads()),
                        conversion.ordersCreated(), sales.paidOrders(),
                        rate(sales.paidOrders(), conversion.ordersCreated())),
                new Deliveries(deliveries.delivered(), deliveries.onTime(),
                        deliveries.delivered() - deliveries.onTime(),
                        rate(deliveries.onTime(), deliveries.delivered())),
                repository.topProducts(fromTs, toTs, TOP_LIMIT),
                repository.topProductPairs(fromTs, toTs, TOP_LIMIT),
                NOTES);
    }

    /** Porcentaje con 2 decimales; 0 si no hay base para calcularlo (evita división entre cero). */
    static BigDecimal rate(long part, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                    "La fecha 'from' (" + start + ") no puede ser posterior a 'to' (" + end + ")");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                    "El rango máximo de consulta es de " + MAX_RANGE_DAYS + " días");
        }
    }
}