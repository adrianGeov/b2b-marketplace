package com.marketplace.b2b.integration;

/**
 * Error al comunicarse con un sistema externo (ERP / CRM). El outbox lo reintenta.
 */
public class IntegrationException extends RuntimeException {

    public IntegrationException(String message) {
        super(message);
    }
}