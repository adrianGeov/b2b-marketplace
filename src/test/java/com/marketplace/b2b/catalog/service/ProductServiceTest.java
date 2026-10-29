package com.marketplace.b2b.catalog.service;

import com.marketplace.b2b.catalog.dto.ProductRequest;
import com.marketplace.b2b.catalog.dto.ProductResponse;
import com.marketplace.b2b.catalog.entity.Product;
import com.marketplace.b2b.catalog.mapper.ProductMapper;
import com.marketplace.b2b.catalog.repository.ProductRepository;
import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Spy
    private ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    @InjectMocks
    private ProductService productService;

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La búsqueda devuelve la página mapeada a DTO con sus metadatos")
    @SuppressWarnings("unchecked")
    void shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("price"));
        given(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(product(1L, "CAF-001", true), product(2L, "CAF-002", true)),
                        pageable, 5));

        PageResponse<ProductResponse> result = productService.search("cafe", "Cafetería", pageable);

        assertThat(result.content()).extracting(ProductResponse::sku).containsExactly("CAF-001", "CAF-002");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.last()).isFalse();
    }

    @Test
    @DisplayName("Ordenar por un campo no permitido lanza INVALID_PARAMETER sin consultar la BD")
    void shouldRejectSortByUnknownField() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("version"));

        assertThatThrownBy(() -> productService.search(null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARAMETER);

        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("Un producto inactivo no es visible para clientes (404)")
    void shouldHideInactiveProductFromCustomers() {
        given(productRepository.findById(5L)).willReturn(Optional.of(product(5L, "TEC-001", false)));

        assertThatThrownBy(() -> productService.getById(5L, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("El ADMIN sí puede consultar un producto inactivo")
    void shouldShowInactiveProductToAdmin() {
        given(productRepository.findById(5L)).willReturn(Optional.of(product(5L, "TEC-001", false)));

        ProductResponse response = productService.getById(5L, true);

        assertThat(response.sku()).isEqualTo("TEC-001");
        assertThat(response.active()).isFalse();
    }

    // ------------------------------------------------------------------
    // Alta / actualización / baja
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Alta: normaliza el SKU a mayúsculas y deja el producto activo por defecto")
    void shouldCreateProduct() {
        ProductRequest request = request(" caf-010 ", null);
        given(productRepository.existsBySku("CAF-010")).willReturn(false);
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> {
            Product toSave = invocation.getArgument(0);
            toSave.setId(20L);
            return toSave;
        });

        ProductResponse response = productService.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getSku()).isEqualTo("CAF-010");
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.name()).isEqualTo("Café en grano 1 kg");
        assertThat(response.price()).isEqualByComparingTo("350.00");
    }

    @Test
    @DisplayName("Alta con SKU existente lanza SKU_ALREADY_EXISTS")
    void shouldRejectDuplicatedSkuOnCreate() {
        given(productRepository.existsBySku("CAF-010")).willReturn(true);

        assertThatThrownBy(() -> productService.create(request("CAF-010", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_ALREADY_EXISTS);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar cambiando a un SKU que ya usa otro producto lanza SKU_ALREADY_EXISTS")
    void shouldRejectDuplicatedSkuOnUpdate() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product(1L, "CAF-001", true)));
        given(productRepository.existsBySku("CAF-002")).willReturn(true);

        assertThatThrownBy(() -> productService.update(1L, request("caf-002", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Actualizar aplica los cambios y respeta 'active' solo si se envía")
    void shouldUpdateProduct() {
        Product existing = product(1L, "CAF-001", true);
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));

        ProductResponse response = productService.update(1L, request("CAF-001", null));

        assertThat(existing.getName()).isEqualTo("Café en grano 1 kg");
        assertThat(existing.getPrice()).isEqualByComparingTo("350.00");
        assertThat(existing.isActive()).isTrue();
        assertThat(response.stock()).isEqualTo(80);
    }

        @Test
    @DisplayName("La baja es lógica: el producto queda inactivo, no se borra")
    void shouldDeactivateProduct() {
        Product existing = product(1L, "CAF-001", true);
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));

        productService.deactivate(1L);

        assertThat(existing.isActive()).isFalse();
        verify(productRepository, never()).delete(any(Product.class));
        verify(productRepository, never()).deleteById(any());
    }

    // ------------------------------------------------------------------
    // Datos de prueba
    // ------------------------------------------------------------------

    private static Product product(Long id, String sku, boolean active) {
        return Product.builder()
                .id(id)
                .sku(sku)
                .name("Producto " + sku)
                .description("Descripción " + sku)
                .category("Cafetería")
                .price(new BigDecimal("100.00"))
                .stock(10)
                .active(active)
                .build();
    }

    private static ProductRequest request(String sku, Boolean active) {
        return new ProductRequest(sku, "Café en grano 1 kg", "Café de altura", "Cafetería",
                new BigDecimal("350.00"), 80, active);
    }
}
