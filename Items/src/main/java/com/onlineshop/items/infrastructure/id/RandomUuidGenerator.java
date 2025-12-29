package com.onlineshop.items.infrastructure.id;

import com.onlineshop.items.domain.service.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Infrastructure implementation of IdGenerator using random UUIDs.
 */
@Component
public class RandomUuidGenerator implements IdGenerator {

    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
