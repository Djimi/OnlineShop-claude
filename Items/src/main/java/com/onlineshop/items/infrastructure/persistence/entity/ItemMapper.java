package com.onlineshop.items.infrastructure.persistence.entity;

import com.onlineshop.common.domain.valueobject.ItemDescription;
import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.common.domain.valueobject.ItemName;
import com.onlineshop.common.domain.valueobject.Quantity;
import com.onlineshop.items.domain.Item;

import org.springframework.stereotype.Component;

/**
 * Mapper between domain Item aggregate and ItemJpaEntity.
 */
@Component
public class ItemMapper {

    public ItemJpaEntity toEntity(Item item) {
        return new ItemJpaEntity(
            item.getId() != null ? item.getId().getValue() : null,
            item.getName().value(),
            item.getQuantity().amount(),
            item.getDescription() != null ? item.getDescription().value() : null
        );
    }

    public Item toDomain(ItemJpaEntity entity) {
        return Item.fromPersistence(
            new ItemId(entity.getId()),
            new ItemName(entity.getName()),
            new Quantity(entity.getQuantity()),
            new ItemDescription(entity.getDescription())
        );
    }
}
