package com.onlineshop.items.application.usecase;

import com.onlineshop.items.application.command.UpdateItemCommand;
import com.onlineshop.items.application.dto.UpdateItemResponse;
import com.onlineshop.items.application.dto.mapper.ItemResponseMapper;
import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.exception.ItemNotFoundException;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.domain.valueobject.ItemDescription;
import com.onlineshop.items.domain.valueobject.ItemId;
import com.onlineshop.items.domain.valueobject.ItemName;
import com.onlineshop.items.domain.valueobject.Quantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateItemUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateItemUseCase.class);

    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ItemResponseMapper mapper;

    public UpdateItemUseCase(ItemRepository itemRepository, ApplicationEventPublisher eventPublisher,
                             ItemResponseMapper mapper) {
        this.itemRepository = itemRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
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

        return mapper.toUpdateItemResponse(savedItem);
    }

    private void publishDomainEvents(Item item) {
        item.getDomainEvents().forEach(event -> {
            log.info("Publishing domain event: {}", event);
            eventPublisher.publishEvent(event);
        });
        item.clearDomainEvents();
    }
}
