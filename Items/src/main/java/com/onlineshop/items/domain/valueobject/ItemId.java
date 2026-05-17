package com.onlineshop.items.domain.valueobject;

import com.onlineshop.common.domain.valueobject.BaseId;

import java.util.UUID;

public class ItemId extends BaseId<UUID> {

    public ItemId(UUID value) {
        super(value);
    }
}
