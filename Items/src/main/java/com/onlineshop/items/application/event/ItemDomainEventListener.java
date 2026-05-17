package com.onlineshop.items.application.event;

import com.onlineshop.items.domain.event.ItemCreated;
import com.onlineshop.items.domain.event.ItemDeleted;
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
    public void onItemCreated(ItemCreated event) {
        log.info("Item created: id={}, name={}, quantity={}, description={}",
                event.getItemId().getValue(),
                event.getName().value(),
                event.getQuantity().amount(),
                event.getDescription() != null ? event.getDescription().value() : null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemUpdated(ItemUpdated event) {
        log.info("Item updated: id={}, name={}, quantity={}, description={}",
                event.getItemId().getValue(),
                event.getName().value(),
                event.getQuantity().amount(),
                event.getDescription() != null ? event.getDescription().value() : null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemDeleted(ItemDeleted event) {
        log.info("Item deleted: id={}", event.getItemId().getValue());
    }
}
