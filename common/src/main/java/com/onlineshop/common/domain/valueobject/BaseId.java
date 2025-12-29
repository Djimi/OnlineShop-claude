package com.onlineshop.common.domain.valueobject;

import lombok.Data;

@Data
public abstract class BaseId<T> {
    private final T value;

    protected BaseId(T value) {
        this.value = value;
    }
}
