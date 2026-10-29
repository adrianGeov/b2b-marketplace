package com.marketplace.b2b.payment.controller;

import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.payment.dto.PaymentRequest;
import com.marketplace.b2b.payment.dto.PaymentResponse;
import com.marketplace.b2b.payment.service.PaymentService;
import com.marketplace.b2b.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderNumber}/payments")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_PAYMENTS)
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Pagar pedido",
            description = "Cobra con la pasarela simulada, con reintentos y backoff ante errores intermitentes. "
                    + "Tokens de prueba: `tok_visa` (aprobado), `tok_declined` (rechazado), "
                    + "`tok_gateway_down` (pasarela caída). Al aprobarse, el pedido pasa a PAID y se sincroniza "
                    + "con ERP/CRM en segundo plano.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago aprobado; pedido PAID"),
            @ApiResponse(responseCode = "422", description = "Pago rechazado o pedido ya pagado / cancelado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Pasarela no disponible tras los reintentos; "
                    + "el pedido sigue reservado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(AuthenticatedUser.from(jwt).id(), orderNumber, request);
    }

    @GetMapping
    @Operation(summary = "Historial de intentos de pago",
            description = "Todos los intentos (aprobados, rechazados y fallidos) del pedido.")
    public List<PaymentResponse> payments(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return paymentService.getPayments(user.id(), orderNumber, user.isAdmin());
    }
}