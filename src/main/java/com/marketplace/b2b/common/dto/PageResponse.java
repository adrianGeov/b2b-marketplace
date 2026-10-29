package com.marketplace.b2b.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Respuesta paginada estable para el API (no se expone directamente Page de Spring,
 * cuyo JSON no es un contrato garantizado).
 *
 * @param page número de página (inicia en 0)
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}