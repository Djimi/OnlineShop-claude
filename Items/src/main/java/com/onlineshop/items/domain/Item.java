package com.onlineshop.items.domain;

import com.onlineshop.common.domain.entity.AggregateRoot;
import com.onlineshop.common.domain.valueobject.ItemDescription;
import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.common.domain.valueobject.ItemName;
import com.onlineshop.common.domain.valueobject.Quantity;
import lombok.Getter;

@Getter
public class Item extends AggregateRoot<ItemId> {

    private final ItemName name;
    private final Quantity quantity;
    private final ItemDescription description;

    public Item(ItemId id, ItemName name, Quantity quantity, ItemDescription description) {
        super(id);
        this.name = name;
        this.quantity = quantity;
        this.description = description;
    }
}
