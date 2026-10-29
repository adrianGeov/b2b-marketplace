package com.marketplace.b2b.integration.event;

public record CustomerRegisteredPayload(
        Long userId,
        String email,
        String fullName,
        String companyName,
        String taxId,
        String role
) {
}