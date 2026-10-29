package com.marketplace.b2b.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de la documentación OpenAPI (Swagger UI en /swagger-ui.html).
 * Los nombres de los tags están numerados para que Swagger los muestre en el orden del flujo de compra.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    public static final String TAG_AUTH = "1. Autenticación";
    public static final String TAG_USERS = "2. Usuarios";
    public static final String TAG_CATALOG = "3. Catálogo";
    public static final String TAG_CART = "4. Carrito";
    public static final String TAG_ORDERS = "5. Pedidos";
    public static final String TAG_PAYMENTS = "6. Pagos";
    public static final String TAG_ADMIN_ORDERS = "7. Admin · Pedidos";
    public static final String TAG_ADMIN_INTEGRATIONS = "8. Admin · Integraciones ERP/CRM";
    public static final String TAG_ADMIN_METRICS = "9. Admin · Métricas";

    private static final String DESCRIPTION = """
            API REST del MVP de eCommerce **B2B** para distribuidores y clientes autorizados, \
            con integración a **ERP** y **CRM**.

            ### Cómo probar
            1. Ejecuta **POST /api/v1/auth/login** (o registra un usuario) y copia el `accessToken`.
            2. Da clic en **Authorize** (arriba a la derecha), pega el token y confirma.
            3. Flujo de compra: **Catálogo → Carrito → Pedidos → Pagos**.

            ### Usuarios de prueba
            | Rol | Correo | Contraseña |
            |---|---|---|
            | ADMIN | admin@marketplace.com | Admin123! |
            | DISTRIBUTOR (datos demo) | norte@demo.mx | Demo1234 |

            ### Datos de prueba de la pasarela simulada
            `tok_visa` → aprobado · `tok_declined` → rechazado · `tok_gateway_down` → pasarela caída

            ### Errores
            Todos los errores usan el formato **ProblemDetail (RFC 9457)** con `code`, `detail`, \
            `correlationId` y, en validaciones, la lista `errors` por campo.
            """;

    @Bean
    public OpenAPI b2bMarketplaceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("B2B Marketplace API")
                        .version("v1")
                        .description(DESCRIPTION))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtenido en /api/v1/auth/login o /api/v1/auth/register")))
                // Por defecto todos los endpoints requieren token; los públicos lo quitan con @SecurityRequirements
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .tags(List.of(
                        new Tag().name(TAG_AUTH).description("Registro e inicio de sesión (públicos)"),
                        new Tag().name(TAG_USERS).description("Perfil del usuario autenticado"),
                        new Tag().name(TAG_CATALOG).description("Consulta de productos; alta y edición solo ADMIN"),
                        new Tag().name(TAG_CART).description("Carrito del usuario autenticado"),
                        new Tag().name(TAG_ORDERS).description("Creación idempotente, consulta y cancelación de pedidos"),
                        new Tag().name(TAG_PAYMENTS).description("Pago con pasarela simulada (reintentos y anti cobro doble)"),
                        new Tag().name(TAG_ADMIN_ORDERS).description("Envío y entrega de pedidos (ADMIN)"),
                        new Tag().name(TAG_ADMIN_INTEGRATIONS)
                                .description("Monitoreo del outbox y simulación de caídas del ERP/CRM (ADMIN)"),
                        new Tag().name(TAG_ADMIN_METRICS).description("Indicadores de negocio del eCommerce (ADMIN)")));
    }
}