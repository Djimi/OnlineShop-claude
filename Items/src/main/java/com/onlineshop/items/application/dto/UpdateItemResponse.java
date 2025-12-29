package com.onlineshop.items.application.dto;

import java.util.UUID;

public record UpdateItemResponse(
    UUID id,
    String name,
    int quantity,
    String description
) {}
