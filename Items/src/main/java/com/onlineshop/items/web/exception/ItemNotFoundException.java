package com.onlineshop.items.web.exception;

import java.util.UUID;

/**
 * Thrown when a requested item is not found.
 * Only accepts safe, structured parameters (like ID) to prevent accidental exposure of internal details.
 */
public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(UUID id) {
        super("Item not found with id: " + id);
    }
}
