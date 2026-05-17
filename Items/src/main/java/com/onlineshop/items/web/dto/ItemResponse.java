package com.onlineshop.items.web.dto;

import java.util.UUID;

public record ItemResponse(
    UUID id,
    String name,
    int quantity,
    String description
) {
}
