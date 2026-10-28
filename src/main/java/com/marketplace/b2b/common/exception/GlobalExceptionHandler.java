package com.marketplace.b2b.common.exception;

import com.marketplace.b2b.common.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // Errores de negocio de la aplicación
    // ------------------------------------------------------------------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getErrorCode(), ex.getMessage(), request, List.of(), ex);
    }

    // ------------------------------------------------------------------
    // Validaciones (@Valid en DTOs y en parámetros)
    // ------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.add(new FieldErrorDetail(fe.getField(), fe.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.add(new FieldErrorDetail(ge.getObjectName(), ge.getDefaultMessage())));
        return build(ErrorCode.VALIDATION_ERROR, validationDetail(errors.size()), request, errors, ex);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex,
                                                                HttpServletRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            result.getResolvableErrors()
                    .forEach(error -> errors.add(new FieldErrorDetail(parameter, error.getDefaultMessage())));
        });
        return build(ErrorCode.VALIDATION_ERROR, validationDetail(errors.size()), request, errors, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, validationDetail(errors.size()), request, errors, ex);
    }

    // ------------------------------------------------------------------
    // Errores de la petición HTTP
    // ------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST,
                "El cuerpo de la petición no es un JSON válido o contiene tipos de dato incorrectos",
                request, List.of(), ex);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingHeader(MissingRequestHeaderException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.MISSING_PARAMETER,
                "Falta el header obligatorio '" + ex.getHeaderName() + "'", request, List.of(), ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request) {
        return build(ErrorCode.MISSING_PARAMETER,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", request, List.of(), ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(ErrorCode.INVALID_PARAMETER,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido: '" + ex.getValue() + "'",
                request, List.of(), ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está soportado en esta ruta", request, List.of(), ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND,
                "No existe la ruta " + request.getRequestURI(), request, List.of(), ex);
    }

    // ------------------------------------------------------------------
    // Persistencia
    // ------------------------------------------------------------------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return build(ErrorCode.CONCURRENT_MODIFICATION,
                "El recurso fue modificado por otra operación. Intente nuevamente", request, List.of(), ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        // El mensaje real de la BD se registra en el log, pero no se expone al cliente
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_INTEGRITY_VIOLATION,
                "La operación viola una restricción de datos", request, List.of(), ex);
    }

    // ------------------------------------------------------------------
    // Seguridad
    // ------------------------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {
        return build(ErrorCode.INVALID_CREDENTIALS, "Correo o contraseña incorrectos", request, List.of(), ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCESS_DENIED,
                "No tiene permisos para realizar esta operación", request, List.of(), ex);
    }

    // ------------------------------------------------------------------
    // Cualquier otro error no contemplado
    // ------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(ErrorCode.INTERNAL_ERROR,
                "Ocurrió un error inesperado. Reporte el correlationId a soporte", request, List.of(), ex);
    }

    // ------------------------------------------------------------------
    // Construcción de la respuesta
    // ------------------------------------------------------------------

    private ResponseEntity<ProblemDetail> build(ErrorCode code, String detail, HttpServletRequest request,
                                                List<FieldErrorDetail> errors, Exception ex) {
        HttpStatus status = code.getStatus();

        if (status.is5xxServerError()) {
            // Errores del servidor: se registra el stack trace completo
            log.error("Error {} en {} {}: {}", code, request.getMethod(), request.getRequestURI(), detail, ex);
        } else {
            // Errores del cliente: basta con una línea, sin stack trace
            log.warn("Error {} en {} {}: {}", code, request.getMethod(), request.getRequestURI(), detail);
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(code.getTitle());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).body(problem);
    }

    private static String validationDetail(int count) {
        return "La petición contiene " + count + " error(es) de validación";
    }
}