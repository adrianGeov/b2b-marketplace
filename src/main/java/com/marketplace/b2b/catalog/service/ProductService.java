package com.marketplace.b2b.catalog.service;

import com.marketplace.b2b.catalog.dto.ProductRequest;
import com.marketplace.b2b.catalog.dto.ProductResponse;
import com.marketplace.b2b.catalog.entity.Product;
import com.marketplace.b2b.catalog.mapper.ProductMapper;
import com.marketplace.b2b.catalog.repository.ProductRepository;
import com.marketplace.b2b.catalog.repository.ProductSpecifications;
import com.marketplace.b2b.common.dto.PageResponse;
import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    /** Campos por los que se permite ordenar (lista blanca). */
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "price", "category", "sku", "stock", "createdAt");

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /** Búsqueda para clientes: solo productos activos. */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, String category, Pageable pageable) {
        return search(search, category, false, pageable);
    }

    /**
     * @param includeInactive true solo para ADMIN (lo decide el controller): incluye productos desactivados
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, String category, boolean includeInactive,
                                                Pageable pageable) {
        validateSort(pageable.getSort());

        List<Specification<Product>> filters = new ArrayList<>();
        if (!includeInactive) {
            filters.add(ProductSpecifications.isActive());
        }
        if (StringUtils.hasText(search)) {
            filters.add(ProductSpecifications.matchesText(search.trim()));
        }
        if (StringUtils.hasText(category)) {
            filters.add(ProductSpecifications.hasCategory(category.trim()));
        }

        return PageResponse.from(
                productRepository.findAll(Specification.allOf(filters), pageable).map(productMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id, boolean includeInactive) {
        Product product = productRepository.findById(id)
                .filter(found -> includeInactive || found.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findActiveCategories();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySku(sku)) {
            throw skuAlreadyExists(sku);
        }

        Product product = productMapper.toEntity(request);
        product.setSku(sku);
        product.setActive(request.active() == null || request.active());

        Product saved = productRepository.save(product);
        log.info("Producto creado id={} sku={}", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        String sku = normalizeSku(request.sku());
        if (!sku.equals(product.getSku()) && productRepository.existsBySku(sku)) {
            throw skuAlreadyExists(sku);
        }

        productMapper.updateEntity(request, product);
        product.setSku(sku);
        if (request.active() != null) {
            product.setActive(request.active());
        }

        log.info("Producto actualizado id={} sku={}", product.getId(), product.getSku());
        return productMapper.toResponse(product);
    }

    /** Baja lógica: se conserva para no romper pedidos históricos. */
    @Transactional
    public void deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        product.setActive(false);
        log.info("Producto desactivado id={} sku={}", product.getId(), product.getSku());
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                        "No se puede ordenar por '" + order.getProperty() + "'. Campos permitidos: "
                                + String.join(", ", SORTABLE_FIELDS.stream().sorted().toList()));
            }
        }
    }

    private static String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private static BusinessException skuAlreadyExists(String sku) {
        return new BusinessException(ErrorCode.SKU_ALREADY_EXISTS, "Ya existe un producto con el SKU " + sku);
    }
}