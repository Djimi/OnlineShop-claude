package com.onlineshop.common.domain.valueobject;

import java.util.UUID;

public class ItemId extends BaseId<UUID> {

    public ItemId(UUID value) {
        super(value);
    }
}
