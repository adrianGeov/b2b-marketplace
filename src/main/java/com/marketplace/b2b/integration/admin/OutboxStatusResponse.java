package com.marketplace.b2b.integration.admin;

public record OutboxStatusResponse(
        long pending,
        long processed,
        long failed,
        boolean erpAvailable,
        boolean crmAvailable
) {
}