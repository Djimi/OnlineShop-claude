package com.onlineshop.common.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public abstract class BaseEntity<ID> {
    private final ID id;
}
