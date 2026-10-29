package com.marketplace.b2b.security;

import com.marketplace.b2b.user.entity.Role;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Usuario autenticado extraído del JWT. Se usa en los controllers con @AuthenticationPrincipal Jwt.
 */
public record AuthenticatedUser(Long id, String email, List<String> roles) {

    public static AuthenticatedUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(JwtTokenService.ROLES_CLAIM);
        return new AuthenticatedUser(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString(JwtTokenService.EMAIL_CLAIM),
                roles == null ? List.of() : roles);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN.name());
    }
}