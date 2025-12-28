package com.onlineshop.items.application.usecase;

import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.items.application.command.DeleteItemCommand;
import com.onlineshop.items.domain.Item;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.web.exception.ItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for deleting an item.
 */
@Service
public class DeleteItemUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteItemUseCase.class);

    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeleteItemUseCase(ItemRepository itemRepository, ApplicationEventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(DeleteItemCommand command) {
        ItemId itemId = new ItemId(command.id());
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(command.id()));

        item.markAsDeleted();

        itemRepository.delete(item);

        publishDomainEvents(item);
    }

    private void publishDomainEvents(Item item) {
        item.getDomainEvents().forEach(event -> {
            log.info("Publishing domain event: {}", event);
            eventPublisher.publishEvent(event);
        });
        item.clearDomainEvents();
    }
}
