package com.onlineshop.items.application.usecase;

import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.query.GetItemQuery;
import com.onlineshop.items.domain.Item;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.web.exception.ItemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for retrieving a single item by ID.
 */
@Service
public class GetItemUseCase {

    private final ItemRepository itemRepository;

    public GetItemUseCase(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public GetItemResponse execute(GetItemQuery query) {
        ItemId itemId = new ItemId(query.id());
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(query.id()));

        return toResponse(item);
    }

    private GetItemResponse toResponse(Item item) {
        return new GetItemResponse(
            item.getId() != null ? item.getId().getValue() : null,
            item.getName().value(),
            item.getQuantity().amount(),
            item.getDescription() != null ? item.getDescription().value() : null
        );
    }
}
