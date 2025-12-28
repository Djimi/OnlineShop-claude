package com.onlineshop.items.application.command;

/**
 * Command for updating an existing item.
 */
public record UpdateItemCommand(
    Long id,
    String name,
    int quantity,
    String description
) {
}
