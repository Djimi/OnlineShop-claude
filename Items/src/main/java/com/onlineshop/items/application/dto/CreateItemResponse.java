package com.onlineshop.items.application.dto;

import java.util.UUID;

public record CreateItemResponse(
    UUID id,
    String name,
    int quantity,
    String description
) {}
