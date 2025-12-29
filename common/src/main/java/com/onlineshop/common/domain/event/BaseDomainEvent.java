package com.onlineshop.common.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for domain events providing common fields.
 */
public abstract class BaseDomainEvent implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected BaseDomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }
}
