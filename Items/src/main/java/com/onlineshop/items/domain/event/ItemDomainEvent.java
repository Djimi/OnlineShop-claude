package com.onlineshop.items.domain.event;

import com.onlineshop.common.domain.event.DomainEvent;

public sealed interface ItemDomainEvent extends DomainEvent
        permits ItemCreated, ItemUpdated, ItemDeleted {
}
