package com.onlineshop.items.domain.aggregateroots;

import com.onlineshop.common.domain.entity.AggregateRoot;
import com.onlineshop.common.domain.valueobject.ItemDescription;
import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.common.domain.valueobject.ItemName;
import com.onlineshop.common.domain.valueobject.Quantity;
import com.onlineshop.items.domain.event.ItemCreated;
import com.onlineshop.items.domain.event.ItemDeleted;
import com.onlineshop.items.domain.event.ItemUpdated;

import java.util.Objects;

/**
 * Item aggregate root representing a product in the catalog.
 */
public class Item extends AggregateRoot<ItemId> {

    private ItemName name;
    private Quantity quantity;
    private ItemDescription description;

    private Item(ItemId id, ItemName name, Quantity quantity, ItemDescription description) {
        super(id);
        validateInvariants(name, quantity);
        this.name = name;
        this.quantity = quantity;
        this.description = description;
    }

    /**
     * Factory method for creating a new item with a pre-assigned ID.
     */
    public static Item createNew(ItemId id, ItemName name, Quantity quantity, ItemDescription description) {
        if (id == null) {
            throw new IllegalArgumentException("Item ID is required");
        }
        var item = new Item(id, name, quantity, description);
        item.registerEvent(new ItemCreated(id, name, quantity, description));
        return item;
    }

    /**
     * Reconstitution method for loading an existing item from persistence.
     */
    public static Item fromPersistence(ItemId id, ItemName name, Quantity quantity, ItemDescription description) {
        return new Item(id, name, quantity, description);
    }

    /**
     * Updates item details. Registers ItemUpdated event if any value changed.
     */
    public void updateDetails(ItemName newName, Quantity newQuantity, ItemDescription newDescription) {
        validateInvariants(newName, newQuantity);

        boolean changed = !Objects.equals(this.name, newName) ||
                          !Objects.equals(this.quantity, newQuantity) ||
                          !Objects.equals(this.description, newDescription);

        if (changed) {
            this.name = newName;
            this.quantity = newQuantity;
            this.description = newDescription;
            registerEvent(new ItemUpdated(getId(), newName, newQuantity, newDescription));
        }
    }

    /**
     * Marks the item as deleted. Registers ItemDeleted event.
     */
    public void markAsDeleted() {
        registerEvent(new ItemDeleted(getId()));
    }

    public ItemName getName() {
        return name;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public ItemDescription getDescription() {
        return description;
    }

    private void validateInvariants(ItemName name, Quantity quantity) {
        if (name == null) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
    }
}
