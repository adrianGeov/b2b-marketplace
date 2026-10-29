package com.marketplace.b2b.catalog.mapper;

import com.marketplace.b2b.catalog.dto.ProductRequest;
import com.marketplace.b2b.catalog.dto.ProductResponse;
import com.marketplace.b2b.catalog.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    /** id y version los controla JPA; active y sku los normaliza el servicio. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);
}