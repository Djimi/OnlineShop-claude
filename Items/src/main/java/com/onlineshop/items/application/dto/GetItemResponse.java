package com.onlineshop.items.application.dto;

public record GetItemResponse(
    Long id,
    String name,
    int quantity,
    String description
) {}
