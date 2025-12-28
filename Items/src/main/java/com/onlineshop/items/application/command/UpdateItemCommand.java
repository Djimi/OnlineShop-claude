package com.onlineshop.items.application.command;

/**
 * Command for updating an existing item.
 * Note: id is provided separately from the path parameter.
 */
public record UpdateItemCommand(
    String name,
    int quantity,
    String description
) {
}
