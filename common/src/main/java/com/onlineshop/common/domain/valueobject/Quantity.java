package com.onlineshop.common.domain.valueobject;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Quantity {
    private final int amount;

    public boolean isGreaterThanZero() {
        return amount > 0;
    }

}
