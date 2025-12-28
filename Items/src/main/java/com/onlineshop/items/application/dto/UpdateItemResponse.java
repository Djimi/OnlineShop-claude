package com.onlineshop.items.application.dto;

public record UpdateItemResponse(
    Long id,
    String name,
    int quantity,
    String description
) {}
