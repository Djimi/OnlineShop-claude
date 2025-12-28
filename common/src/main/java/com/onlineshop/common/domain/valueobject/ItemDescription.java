package com.onlineshop.common.domain.valueobject;

public record ItemDescription(String value) {

    private static final int MAX_LENGTH = 500;

    public ItemDescription {
        if (value != null && value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Item description too long (max " + MAX_LENGTH + " characters)");
        }
    }
}
