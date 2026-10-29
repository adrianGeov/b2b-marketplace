package com.marketplace.b2b.metrics.service;

import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.metrics.dto.MetricsResponse;
import com.marketplace.b2b.metrics.dto.MetricsResponse.TopProduct;
import com.marketplace.b2b.metrics.repository.MetricsRepository;
import com.marketplace.b2b.metrics.repository.MetricsRepository.ConversionCounts;
import com.marketplace.b2b.metrics.repository.MetricsRepository.DeliveryCounts;
import com.marketplace.b2b.metrics.repository.MetricsRepository.SalesSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-28T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private MetricsRepository repository;

    private MetricsService service;

    @BeforeEach
    void setUp() {
        service = new MetricsService(repository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Calcula ticket promedio, conversión y entregas a tiempo con 2 decimales")
    void shouldBuildMetricsWithRates() {
        given(repository.salesSummary(any(), any()))
                .willReturn(new SalesSummary(13, new BigDecimal("76418.50"), new BigDecimal("5878.3461538")));
        given(repository.conversionCounts(any(), any())).willReturn(new ConversionCounts(6, 4, 15));
        given(repository.deliveryCounts(any(), any())).willReturn(new DeliveryCounts(10, 7));
        given(repository.topProducts(any(), any(), anyInt())).willReturn(
                List.of(new TopProduct("PAP-001", "Caja de hojas", 42, new BigDecimal("37758.00"))));
        given(repository.topProductPairs(any(), any(), anyInt())).willReturn(List.of());

        MetricsResponse metrics = service.getMetrics(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 27));

        assertThat(metrics.sales().averageTicket()).isEqualByComparingTo("5878.35");
        assertThat(metrics.conversion().leadConversionRate()).isEqualByComparingTo("66.67");   // 4 de 6
        assertThat(metrics.conversion().checkoutConversionRate()).isEqualByComparingTo("86.67"); // 13 de 15
        assertThat(metrics.deliveries().late()).isEqualTo(3);
        assertThat(metrics.deliveries().onTimeRate()).isEqualByComparingTo("70.00");
        assertThat(metrics.topProducts()).extracting(TopProduct::sku).containsExactly("PAP-001");
        assertThat(metrics.notes()).hasSize(2);
    }

    @Test
    @DisplayName("Sin datos en el periodo los porcentajes son 0 (no hay división entre cero)")
    void shouldReturnZeroRatesWhenThereIsNoData() {
        given(repository.salesSummary(any(), any())).willReturn(new SalesSummary(0, BigDecimal.ZERO, BigDecimal.ZERO));
        given(repository.conversionCounts(any(), any())).willReturn(new ConversionCounts(0, 0, 0));
        given(repository.deliveryCounts(any(), any())).willReturn(new DeliveryCounts(0, 0));

        MetricsResponse metrics = service.getMetrics(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(metrics.conversion().leadConversionRate()).isEqualByComparingTo("0");
        assertThat(metrics.conversion().checkoutConversionRate()).isEqualByComparingTo("0");
        assertThat(metrics.deliveries().onTimeRate()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Sin fechas usa los últimos 90 días, con 'to' inclusive")
    void shouldUseLast90DaysByDefault() {
        given(repository.salesSummary(any(), any())).willReturn(new SalesSummary(0, BigDecimal.ZERO, BigDecimal.ZERO));
        given(repository.conversionCounts(any(), any())).willReturn(new ConversionCounts(0, 0, 0));
        given(repository.deliveryCounts(any(), any())).willReturn(new DeliveryCounts(0, 0));

        MetricsResponse metrics = service.getMetrics(null, null);

        assertThat(metrics.period().from()).isEqualTo(LocalDate.of(2026, 7, 30));
        assertThat(metrics.period().to()).isEqualTo(LocalDate.of(2026, 10, 28));
        verify(repository).salesSummary(OffsetDateTime.parse("2026-07-30T00:00Z"),
                OffsetDateTime.parse("2026-10-29T00:00Z"));
    }

    @Test
    @DisplayName("Un rango con 'from' posterior a 'to' se rechaza sin consultar la BD")
    void shouldRejectInvertedRange() {
        assertThatThrownBy(() -> service.getMetrics(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        verifyNoInteractions(repository);
    }
}