package com.onlineshop.items.web.exception;

/**
 * Thrown when a requested item is not found.
 * Only accepts safe, structured parameters (like ID) to prevent accidental exposure of internal details.
 */
public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(Long id) {
        super("Item not found with id: " + id);
    }
}
