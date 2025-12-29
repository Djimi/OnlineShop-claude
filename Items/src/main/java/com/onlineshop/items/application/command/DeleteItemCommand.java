package com.onlineshop.items.application.command;

import java.util.UUID;

/**
 * Command for deleting an item.
 */
public record DeleteItemCommand(UUID id) {
}
