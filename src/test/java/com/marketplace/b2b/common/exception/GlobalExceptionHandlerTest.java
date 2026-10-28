package com.marketplace.b2b.common.exception;

import com.marketplace.b2b.common.logging.CorrelationIdFilter;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private static final String CORRELATION_ID = "test-correlation-id";

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("POST", "/api/v1/orders");
        MDC.put(CorrelationIdFilter.MDC_KEY, CORRELATION_ID);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("BusinessException se traduce al estatus y código definidos en ErrorCode")
    void shouldMapBusinessExceptionToStatusAndCode() {
        BusinessException ex = new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                "Existencias insuficientes para el SKU PAP-001");

        ResponseEntity<ProblemDetail> response = handler.handleBusiness(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTitle()).isEqualTo("Existencias insuficientes");
        assertThat(body.getDetail()).isEqualTo("Existencias insuficientes para el SKU PAP-001");
        assertThat(body.getInstance()).hasToString("/api/v1/orders");
        assertThat(body.getProperties())
                .containsEntry("code", "INSUFFICIENT_STOCK")
                .containsEntry("correlationId", CORRELATION_ID)
                .containsKey("timestamp");
    }

    @Test
    @DisplayName("ResourceNotFoundException responde 404 con detalle descriptivo")
    void shouldReturnNotFoundForMissingResource() {
        ResponseEntity<ProblemDetail> response =
                handler.handleBusiness(new ResourceNotFoundException("Producto", 99), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Producto con identificador '99' no encontrado");
        assertThat(response.getBody().getProperties()).containsEntry("code", "RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("Errores de @Valid se devuelven como lista de campos con su mensaje")
    void shouldListFieldErrorsOnValidationFailure() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "debe ser un correo válido"));
        bindingResult.addError(new FieldError("request", "quantity", "debe ser mayor que 0"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(dummyParameter(), bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleBodyValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getDetail()).isEqualTo("La petición contiene 2 error(es) de validación");
        assertThat(body.getProperties()).containsEntry("code", "VALIDATION_ERROR");
        assertThat(body.getProperties().get("errors"))
                .asInstanceOf(InstanceOfAssertFactories.list(FieldErrorDetail.class))
                .containsExactly(
                        new FieldErrorDetail("email", "debe ser un correo válido"),
                        new FieldErrorDetail("quantity", "debe ser mayor que 0"));
    }

    @Test
    @DisplayName("Header obligatorio faltante responde 400 indicando el nombre del header")
    void shouldReportMissingHeader() throws NoSuchMethodException {
        MissingRequestHeaderException ex = new MissingRequestHeaderException("Idempotency-Key", dummyParameter());

        ResponseEntity<ProblemDetail> response = handler.handleMissingHeader(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains("Idempotency-Key");
    }

    @Test
    @DisplayName("Un error inesperado responde 500 sin exponer detalles internos")
    void shouldHideInternalDetailsOnUnexpectedError() {
        RuntimeException ex = new RuntimeException("NullPointer en clase interna XYZ");

        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).doesNotContain("XYZ");
        assertThat(response.getBody().getProperties())
                .containsEntry("code", "INTERNAL_ERROR")
                .containsEntry("correlationId", CORRELATION_ID);
    }

    private MethodParameter dummyParameter() throws NoSuchMethodException {
        return new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyEndpoint", String.class), 0);
    }

    @SuppressWarnings("unused")
    private void dummyEndpoint(String body) {
        // Método auxiliar solo para construir un MethodParameter en las pruebas
    }
}