package com.marketplace.b2b.catalog.controller;

import com.marketplace.b2b.catalog.dto.ProductRequest;
import com.marketplace.b2b.catalog.dto.ProductResponse;
import com.marketplace.b2b.catalog.service.ProductService;
import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
public class ProductController {

    private final ProductService productService;

    /**
     * Ejemplo: /api/v1/products?search=cafe&category=Cafetería&page=0&size=10&sort=price,desc
     */
    @GetMapping
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no debe exceder 100 caracteres")
            String search,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return productService.search(search, category, pageable);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.getCategories();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return productService.getById(id, AuthenticatedUser.from(jwt).isAdmin());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        productService.deactivate(id);
    }
}