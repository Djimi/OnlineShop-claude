package com.onlineshop.items.application.command;

import java.util.UUID;

/**
 * Command for updating an existing item.
 */
public record UpdateItemCommand(
    UUID id,
    String name,
    int quantity,
    String description
) {
}
