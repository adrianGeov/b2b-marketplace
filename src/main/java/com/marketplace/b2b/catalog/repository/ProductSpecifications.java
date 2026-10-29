package com.marketplace.b2b.catalog.repository;

import com.marketplace.b2b.catalog.entity.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
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

    /**
     * Busca el texto en nombre, SKU o descripción sin distinguir mayúsculas NI acentos
     * (unaccent en PostgreSQL; el texto del usuario se normaliza igual en Java).
     */
    public static Specification<Product> matchesText(String text) {
        String pattern = "%" + escapeLike(normalize(text)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(unaccentLower(cb, root.<String>get("name")), pattern, ESCAPE),
                cb.like(unaccentLower(cb, root.<String>get("sku")), pattern, ESCAPE),
                cb.like(unaccentLower(cb, root.<String>get("description")), pattern, ESCAPE));
    }

    /** unaccent(lower(columna)) */
    private static Expression<String> unaccentLower(CriteriaBuilder cb, Expression<String> field) {
        return cb.function("unaccent", String.class, cb.lower(field));
    }

    
    static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Evita que % y _ escritos por el usuario actúen como comodines del LIKE. */
    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}