package com.onlineshop.items.application.usecase;

import com.onlineshop.common.domain.valueobject.ItemDescription;
import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.common.domain.valueobject.ItemName;
import com.onlineshop.common.domain.valueobject.Quantity;
import com.onlineshop.items.application.command.CreateItemCommand;
import com.onlineshop.items.application.dto.CreateItemResponse;
import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.domain.service.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for creating a new item.
 */
@Service
public class CreateItemUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateItemUseCase.class);

    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final IdGenerator idGenerator;

    public CreateItemUseCase(ItemRepository itemRepository, ApplicationEventPublisher eventPublisher,
                             IdGenerator idGenerator) {
        this.itemRepository = itemRepository;
        this.eventPublisher = eventPublisher;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public CreateItemResponse execute(CreateItemCommand command) {
        ItemId id = new ItemId(idGenerator.generate());
        ItemName name = new ItemName(command.name());
        Quantity quantity = new Quantity(command.quantity());
        ItemDescription description = new ItemDescription(command.description());

        Item item = Item.createNew(id, name, quantity, description);
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

    private CreateItemResponse toResponse(Item item) {
        return new CreateItemResponse(
            item.getId() != null ? item.getId().getValue() : null,
            item.getName().value(),
            item.getQuantity().amount(),
            item.getDescription() != null ? item.getDescription().value() : null
        );
    }
}
