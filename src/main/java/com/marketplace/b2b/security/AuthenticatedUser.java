package com.marketplace.b2b.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Usuario autenticado extraído del JWT. Se usa en los controllers con @AuthenticationPrincipal Jwt.
 */
public record AuthenticatedUser(Long id, String email) {

    public static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString(JwtTokenService.EMAIL_CLAIM));
    }
}