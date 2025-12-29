package com.onlineshop.items.application.dto;

import java.util.UUID;

public record GetItemResponse(
    UUID id,
    String name,
    int quantity,
    String description
) {}
