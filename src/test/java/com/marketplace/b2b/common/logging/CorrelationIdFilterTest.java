package com.marketplace.b2b.common.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Genera un correlationId cuando la petición no lo trae")
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idInsideRequest = new AtomicReference<>();
        FilterChain chain = (req, res) -> idInsideRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        String responseId = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(responseId).isNotBlank();
        assertThatCode(() -> UUID.fromString(responseId)).doesNotThrowAnyException();
        assertThat(idInsideRequest.get()).isEqualTo(responseId);
    }

    @Test
    @DisplayName("Reutiliza el correlationId enviado por el cliente si es válido")
    void shouldReuseValidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(CorrelationIdFilter.HEADER, "erp-req-12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("erp-req-12345678");
    }

    @Test
    @DisplayName("Descarta un correlationId con caracteres no permitidos (evita inyección en logs)")
    void shouldReplaceInvalidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader(CorrelationIdFilter.HEADER, "abc\nFAKE LOG LINE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isNotEqualTo("abc\nFAKE LOG LINE")
                .doesNotContain("\n");
    }

    @Test
    @DisplayName("Limpia el MDC al terminar la petición (los hilos del servidor se reutilizan)")
    void shouldClearMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}