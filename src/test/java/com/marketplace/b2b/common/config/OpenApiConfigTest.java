package com.marketplace.b2b.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenAPI openApi = new OpenApiConfig().b2bMarketplaceOpenApi();

    @Test
    @DisplayName("Define el esquema de seguridad Bearer JWT y lo aplica por defecto")
    void shouldDeclareBearerJwtSecurity() {
        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_AUTH);

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(openApi.getSecurity()).hasSize(1);
    }

    @Test
    @DisplayName("Los tags siguen el orden del flujo de compra")
    void shouldListTagsInFlowOrder() {
        assertThat(openApi.getTags()).extracting(Tag::getName).startsWith(
                OpenApiConfig.TAG_AUTH, OpenApiConfig.TAG_USERS, OpenApiConfig.TAG_CATALOG,
                OpenApiConfig.TAG_CART, OpenApiConfig.TAG_ORDERS, OpenApiConfig.TAG_PAYMENTS);
    }
}