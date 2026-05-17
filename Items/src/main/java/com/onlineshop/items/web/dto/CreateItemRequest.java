package com.onlineshop.items.web.dto;

public record CreateItemRequest(
    String name,
    int quantity,
    String description
) {
}
