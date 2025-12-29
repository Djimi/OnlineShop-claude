package com.onlineshop.items.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity for Item persistence.
 * This is an infrastructure concern, separate from the domain model.
 */
@Entity
@Table(name = "items")
public class ItemJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 500)
    private String description;

    protected ItemJpaEntity() {
    }

    public ItemJpaEntity(UUID id, String name, int quantity, String description) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDescription() {
        return description;
    }
}
