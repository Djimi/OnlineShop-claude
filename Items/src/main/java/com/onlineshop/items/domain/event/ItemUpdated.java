package com.onlineshop.items.domain.event;

import com.onlineshop.common.domain.event.BaseDomainEvent;
import com.onlineshop.items.domain.valueobject.ItemDescription;
import com.onlineshop.items.domain.valueobject.ItemId;
import com.onlineshop.items.domain.valueobject.ItemName;
import com.onlineshop.items.domain.valueobject.Quantity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString(callSuper = true)
public final class ItemUpdated extends BaseDomainEvent implements ItemDomainEvent {

    private final ItemId itemId;
    private final ItemName name;
    private final Quantity quantity;
    private final ItemDescription description;
}
