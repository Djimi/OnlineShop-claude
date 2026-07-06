package com.onlineshop.common.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class BaseEntity<ID> {
    private final ID id;
}
