package com.onlineshop.items.domain.event;

import com.onlineshop.common.domain.event.BaseDomainEvent;
import com.onlineshop.items.domain.valueobject.ItemId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString(callSuper = true)
public final class ItemDeleted extends BaseDomainEvent implements ItemDomainEvent {

    private final ItemId itemId;
}
