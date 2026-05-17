package com.onlineshop.items.application.event;

import com.onlineshop.items.domain.event.ItemCreated;
import com.onlineshop.items.domain.event.ItemDeleted;
import com.onlineshop.items.domain.event.ItemDomainEvent;
import com.onlineshop.items.domain.event.ItemUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ItemDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(ItemDomainEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemEvent(ItemDomainEvent event) {
        switch (event) {
            case ItemCreated e -> log.info("Item created: id={}, name={}, quantity={}, description={}",
                    e.getItemId().getValue(),
                    e.getName().value(),
                    e.getQuantity().amount(),
                    e.getDescription().value());
            case ItemUpdated e -> log.info("Item updated: id={}, name={}, quantity={}, description={}",
                    e.getItemId().getValue(),
                    e.getName().value(),
                    e.getQuantity().amount(),
                    e.getDescription().value());
            case ItemDeleted e -> log.info("Item deleted: id={}", e.getItemId().getValue());
        }
    }
}
