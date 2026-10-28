package com.marketplace.b2b.security;

import com.marketplace.b2b.security.JwtTokenService.IssuedToken;
import com.marketplace.b2b.user.entity.Role;
import com.marketplace.b2b.user.entity.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba con el encoder/decoder reales de Nimbus (sin levantar Spring).
 */
class JwtTokenServiceTest {

    private static final String SECRET = "ZpAOc6LHOLnrFVWDU1qv/kzZOcrulnYdwHtH6eyjjiw=";
    private static final String OTHER_SECRET = "M8QzToeO4nTDR7Qa/P8WjS2+IvcRvwvju5DxbONWfIb6rXpC8VRJFahwqyrsOR7B";

    private final JwtProperties properties =
            new JwtProperties(SECRET, "b2b-marketplace-test", Duration.ofMinutes(30));

    private JwtTokenService tokenService;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey())),
                properties);
        decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Test
    @DisplayName("El token contiene id, correo, rol, issuer y la expiración configurada")
    void shouldGenerateTokenWithExpectedClaims() {
        IssuedToken issued = tokenService.generate(user());

        Jwt jwt = decoder.decode(issued.value());

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString(JwtTokenService.EMAIL_CLAIM)).isEqualTo("compras@acme.mx");
        assertThat(jwt.getClaimAsStringList(JwtTokenService.ROLES_CLAIM)).containsExactly("CUSTOMER");
        assertThat(jwt.getClaimAsString(JwtClaimNames.ISS)).isEqualTo("b2b-marketplace-test");
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(issued.expiresInSeconds()).isEqualTo(1800);
    }

    @Test
    @DisplayName("Un token firmado con otra llave es rechazado")
    void shouldRejectTokenSignedWithAnotherKey() {
        String token = tokenService.generate(user()).value();
        JwtDecoder otherDecoder = NimbusJwtDecoder
                .withSecretKey(new JwtProperties(OTHER_SECRET, "x", null).secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        assertThatThrownBy(() -> otherDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un secreto menor a 256 bits se rechaza al iniciar")
    void shouldRejectShortSecret() {
        JwtProperties weak = new JwtProperties("c2hvcnQ=", "x", null); // "short"

        assertThatThrownBy(weak::secretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    private static User user() {
        return User.builder()
                .id(7L)
                .email("compras@acme.mx")
                .role(Role.CUSTOMER)
                .build();
    }
}