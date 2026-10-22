package com.bank.customer.interfaces.rest.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
