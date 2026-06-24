package com.jettch.sisgev.shared.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope padrão de respostas paginadas (BE-05).
 * Formato alinhado ao spec backend §9.1.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
