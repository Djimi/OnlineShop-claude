package com.onlineshop.items.domain.repository;

import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.items.domain.Item;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Item aggregate.
 * This is a domain contract - implementation is in infrastructure layer.
 */
public interface ItemRepository {

    Item save(Item item);

    Optional<Item> findById(ItemId id);

    List<Item> findAll();

    void delete(Item item);

    boolean existsById(ItemId id);
}
