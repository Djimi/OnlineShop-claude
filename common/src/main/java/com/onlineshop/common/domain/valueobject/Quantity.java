package com.onlineshop.common.domain.valueobject;

public record Quantity(int amount) {

    public Quantity {
        if (amount < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    public boolean isGreaterThanZero() {
        return amount > 0;
    }
}
