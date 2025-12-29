package com.onlineshop.items.domain.service;

import java.util.UUID;

/**
 * Domain service interface for generating unique identifiers.
 * Implementation is provided by the infrastructure layer.
 */
public interface IdGenerator {
    UUID generate();
}
