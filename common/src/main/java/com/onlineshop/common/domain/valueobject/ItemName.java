package com.onlineshop.common.domain.valueobject;

public record ItemName(String value) {

    private static final int MAX_LENGTH = 200;

    public ItemName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Item name too long (max " + MAX_LENGTH + " characters)");
        }
    }
}
