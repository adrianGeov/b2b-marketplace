package com.marketplace.b2b.security;

import com.marketplace.b2b.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Emite los JWT de acceso.
 * sub = id del usuario, email = correo, roles = [ROL].
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    public static final String ROLES_CLAIM = "roles";
    public static final String EMAIL_CLAIM = "email";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public IssuedToken generate(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(ROLES_CLAIM, List.of(user.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, properties.expiration().toSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}