package com.marketplace.b2b.payment.controller;

import com.marketplace.b2b.payment.dto.PaymentRequest;
import com.marketplace.b2b.payment.dto.PaymentResponse;
import com.marketplace.b2b.payment.service.PaymentService;
import com.marketplace.b2b.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(AuthenticatedUser.from(jwt).id(), orderNumber, request);
    }

    /** Historial de intentos de pago del pedido (útil para soporte y auditoría). */
    @GetMapping
    public List<PaymentResponse> payments(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return paymentService.getPayments(user.id(), orderNumber, user.isAdmin());
    }
}