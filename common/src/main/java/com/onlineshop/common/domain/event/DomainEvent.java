package com.onlineshop.common.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for domain events.
 * All domain events should implement this interface.
 */
public interface DomainEvent {

    UUID getEventId();

    Instant getOccurredAt();
}
