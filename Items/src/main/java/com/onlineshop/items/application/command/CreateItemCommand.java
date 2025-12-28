package com.onlineshop.items.application.command;

/**
 * Command for creating a new item.
 */
public record CreateItemCommand(
    String name,
    int quantity,
    String description
) {
}
