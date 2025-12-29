package com.onlineshop.items.application.usecase;

import com.onlineshop.common.domain.valueobject.ItemDescription;
import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.common.domain.valueobject.ItemName;
import com.onlineshop.common.domain.valueobject.Quantity;
import com.onlineshop.items.application.command.UpdateItemCommand;
import com.onlineshop.items.application.dto.UpdateItemResponse;
import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.web.exception.ItemNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating an existing item.
 */
@Service
public class UpdateItemUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateItemUseCase.class);

    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateItemUseCase(ItemRepository itemRepository, ApplicationEventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UpdateItemResponse execute(UpdateItemCommand command) {
        ItemId itemId = new ItemId(command.id());
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(command.id()));

        ItemName newName = new ItemName(command.name());
        Quantity newQuantity = new Quantity(command.quantity());
        ItemDescription newDescription = new ItemDescription(command.description());

        item.updateDetails(newName, newQuantity, newDescription);

        Item savedItem = itemRepository.save(item);

        publishDomainEvents(savedItem);

        return toResponse(savedItem);
    }

    private void publishDomainEvents(Item item) {
        item.getDomainEvents().forEach(event -> {
            log.info("Publishing domain event: {}", event);
            eventPublisher.publishEvent(event);
        });
        item.clearDomainEvents();
    }

    private UpdateItemResponse toResponse(Item item) {
        return new UpdateItemResponse(
            item.getId() != null ? item.getId().getValue() : null,
            item.getName().value(),
            item.getQuantity().amount(),
            item.getDescription() != null ? item.getDescription().value() : null
        );
    }
}
