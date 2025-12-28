package com.onlineshop.items.application.dto;

public record CreateItemResponse(
    Long id,
    String name,
    int quantity,
    String description
) {}
