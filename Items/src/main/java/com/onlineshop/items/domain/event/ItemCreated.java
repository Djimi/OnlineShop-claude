package com.onlineshop.items.domain.event;

import com.onlineshop.items.domain.Item;

import java.time.Instant;

public record ItemCreated(Item item, Instant createdAt) {
}
