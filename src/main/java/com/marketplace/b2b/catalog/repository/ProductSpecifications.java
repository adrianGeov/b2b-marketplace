package com.marketplace.b2b.catalog.repository;

import com.marketplace.b2b.catalog.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Filtros dinámicos del catálogo. El servicio solo agrega los que el usuario envía.
 */
public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.<Boolean>get("active"));
    }

    public static Specification<Product> hasCategory(String category) {
        String value = category.toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get("category")), value);
    }

    /** Busca el texto en nombre, SKU o descripción (sin distinguir mayúsculas). */
    public static Specification<Product> matchesText(String text) {
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("name")), pattern, ESCAPE),
                cb.like(cb.lower(root.<String>get("sku")), pattern, ESCAPE),
                cb.like(cb.lower(root.<String>get("description")), pattern, ESCAPE));
    }

    /** Evita que % y _ escritos por el usuario actúen como comodines del LIKE. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}