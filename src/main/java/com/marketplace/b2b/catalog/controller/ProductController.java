package com.marketplace.b2b.catalog.controller;

import com.marketplace.b2b.catalog.dto.ProductRequest;
import com.marketplace.b2b.catalog.dto.ProductResponse;
import com.marketplace.b2b.catalog.service.ProductService;
import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_CATALOG)
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Buscar productos",
            description = "Búsqueda paginada por texto (nombre, SKU o descripción, sin distinguir acentos) "
                    + "y categoría. Orden permitido: name, price, category, sku, stock, createdAt.")
    public PageResponse<ProductResponse> search(
            @Parameter(description = "Texto a buscar", example = "cafe")
            @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no debe exceder 100 caracteres")
            String search,
            @Parameter(description = "Categoría exacta", example = "Cafetería")
            @RequestParam(required = false) String category,
            @Parameter(description = "Solo ADMIN: incluir productos inactivos (para clientes se ignora)")
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        boolean showInactive = includeInactive && AuthenticatedUser.from(jwt).isAdmin();
        return productService.search(search, category, showInactive, pageable);
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorías", description = "Categorías con al menos un producto activo.")
    public List<String> categories() {
        return productService.getCategories();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de producto", description = "Un producto inactivo solo es visible para ADMIN.")
    public ProductResponse getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return productService.getById(id, AuthenticatedUser.from(jwt).isAdmin());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear producto (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado; header Location con su URL"),
            @ApiResponse(responseCode = "403", description = "El usuario no es ADMIN",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "SKU duplicado",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar producto (ADMIN)", description = "Con active=true se reactiva un producto.")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar producto (ADMIN)",
            description = "Baja lógica: el producto se conserva para no afectar pedidos históricos.")
    public void deactivate(@PathVariable Long id) {
        productService.deactivate(id);
    }
}