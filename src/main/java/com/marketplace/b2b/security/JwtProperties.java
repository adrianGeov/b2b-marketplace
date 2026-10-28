package com.marketplace.b2b.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * Propiedades app.security.jwt.* del application.properties.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {

    private static final int MIN_SECRET_BYTES = 32; // 256 bits, requerido por HS256

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("La propiedad app.security.jwt.secret es obligatoria");
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "b2b-marketplace";
        }
        if (expiration == null) {
            expiration = Duration.ofHours(1);
        }
    }

    public SecretKey secretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("El secreto JWT debe tener al menos 256 bits (32 bytes)");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}