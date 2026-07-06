package com.onlineshop.items.web.dto;

public record UpdateItemRequest(
    String name,
    int quantity,
    String description
) {
}
