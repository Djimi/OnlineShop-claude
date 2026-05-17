package com.onlineshop.items.domain;

import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.event.ItemCreated;
import com.onlineshop.items.domain.event.ItemDeleted;
import com.onlineshop.items.domain.event.ItemUpdated;
import com.onlineshop.items.domain.valueobject.ItemDescription;
import com.onlineshop.items.domain.valueobject.ItemId;
import com.onlineshop.items.domain.valueobject.ItemName;
import com.onlineshop.items.domain.valueobject.Quantity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemTest {

    private static final ItemId ID = new ItemId(UUID.randomUUID());
    private static final ItemName NAME = new ItemName("Test Item");
    private static final Quantity QTY = new Quantity(10);
    private static final ItemDescription DESC = new ItemDescription("A test item description");

    @Test
    void createNew_shouldReturnItemWithCorrectStateAndEvent() {
        Item item = Item.createNew(ID, NAME, QTY, DESC);

        assertThat(item.getId()).isEqualTo(ID);
        assertThat(item.getName()).isEqualTo(NAME);
        assertThat(item.getQuantity()).isEqualTo(QTY);
        assertThat(item.getDescription()).isEqualTo(DESC);

        assertThat(item.getDomainEvents()).hasSize(1);
        assertThat(item.getDomainEvents().get(0))
                .isInstanceOf(ItemCreated.class);
    }

    @Test
    void createNew_shouldThrowWhenIdIsNull() {
        assertThatThrownBy(() -> Item.createNew(null, NAME, QTY, DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item ID is required");
    }

    @Test
    void createNew_shouldThrowWhenNameIsNull() {
        assertThatThrownBy(() -> Item.createNew(ID, null, QTY, DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item name is required");
    }

    @Test
    void createNew_shouldThrowWhenQuantityIsNull() {
        assertThatThrownBy(() -> Item.createNew(ID, NAME, null, DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity is required");
    }

    @Test
    void createNew_withNullDescription_shouldDefaultToEmpty() {
        Item item = Item.createNew(ID, NAME, QTY, null);

        assertThat(item.getDescription()).isNotNull();
        assertThat(item.getDescription().value()).isEmpty();
    }

    @Test
    void fromPersistence_shouldReconstituteWithoutEvents() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        assertThat(item.getId()).isEqualTo(ID);
        assertThat(item.getName()).isEqualTo(NAME);
        assertThat(item.getQuantity()).isEqualTo(QTY);
        assertThat(item.getDescription()).isEqualTo(DESC);
        assertThat(item.getDomainEvents()).isEmpty();
    }

    @Test
    void updateDetails_shouldUpdateFieldsAndRegisterEvent() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);
        ItemName newName = new ItemName("Updated Item");
        Quantity newQty = new Quantity(20);
        ItemDescription newDesc = new ItemDescription("Updated description");

        item.clearDomainEvents();
        item.updateDetails(newName, newQty, newDesc);

        assertThat(item.getName()).isEqualTo(newName);
        assertThat(item.getQuantity()).isEqualTo(newQty);
        assertThat(item.getDescription()).isEqualTo(newDesc);

        assertThat(item.getDomainEvents()).hasSize(1);
        assertThat(item.getDomainEvents().get(0)).isInstanceOf(ItemUpdated.class);
    }

    @Test
    void updateDetails_shouldNotRegisterEventWhenNothingChanged() {
        Item item = Item.createNew(ID, NAME, QTY, DESC);
        item.clearDomainEvents();

        item.updateDetails(NAME, QTY, DESC);

        assertThat(item.getDomainEvents()).isEmpty();
    }

    @Test
    void markAsDeleted_shouldRegisterItemDeletedEvent() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        item.markAsDeleted();

        assertThat(item.getDomainEvents()).hasSize(1);
        assertThat(item.getDomainEvents().get(0)).isInstanceOf(ItemDeleted.class);
    }

    @Test
    void increaseStock_shouldAddQuantity() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        item.increaseStock(new Quantity(5));

        assertThat(item.getQuantity().amount()).isEqualTo(15);
    }

    @Test
    void decreaseStock_shouldReduceQuantity() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        item.decreaseStock(new Quantity(4));

        assertThat(item.getQuantity().amount()).isEqualTo(6);
    }

    @Test
    void decreaseStock_shouldThrowWhenInsufficientStock() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        assertThatThrownBy(() -> item.decreaseStock(new Quantity(11)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void decreaseStock_shouldAllowExactlyToZero() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        item.decreaseStock(new Quantity(10));

        assertThat(item.getQuantity().amount()).isZero();
    }

    @Test
    void reserveStock_shouldReduceQuantity() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        item.reserveStock(new Quantity(3));

        assertThat(item.getQuantity().amount()).isEqualTo(7);
    }

    @Test
    void reserveStock_shouldThrowWhenInsufficientStock() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        assertThatThrownBy(() -> item.reserveStock(new Quantity(11)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void createdEvent_shouldCarryCorrectFields() {
        Item item = Item.createNew(ID, NAME, QTY, DESC);
        ItemCreated event = (ItemCreated) item.getDomainEvents().get(0);

        assertThat(event.getItemId()).isEqualTo(ID);
        assertThat(event.getName()).isEqualTo(NAME);
        assertThat(event.getQuantity()).isEqualTo(QTY);
        assertThat(event.getDescription()).isEqualTo(DESC);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void deletedEvent_shouldCarryCorrectFields() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);

        item.markAsDeleted();

        ItemDeleted event = (ItemDeleted) item.getDomainEvents().get(0);
        assertThat(event.getItemId()).isEqualTo(ID);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void updatedEvent_shouldCarryCorrectFields() {
        Item item = Item.fromPersistence(ID, NAME, QTY, DESC);
        ItemName newName = new ItemName("Updated");
        Quantity newQty = new Quantity(5);
        ItemDescription newDesc = new ItemDescription("Updated desc");

        item.clearDomainEvents();
        item.updateDetails(newName, newQty, newDesc);

        ItemUpdated event = (ItemUpdated) item.getDomainEvents().get(0);
        assertThat(event.getItemId()).isEqualTo(ID);
        assertThat(event.getName()).isEqualTo(newName);
        assertThat(event.getQuantity()).isEqualTo(newQty);
        assertThat(event.getDescription()).isEqualTo(newDesc);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
    }
}
