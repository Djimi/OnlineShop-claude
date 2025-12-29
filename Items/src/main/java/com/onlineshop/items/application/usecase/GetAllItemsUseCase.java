package com.onlineshop.items.application.usecase;

import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.query.GetAllItemsQuery;
import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for retrieving all items.
 */
@Service
public class GetAllItemsUseCase {

    private final ItemRepository itemRepository;

    public GetAllItemsUseCase(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<GetItemResponse> execute(GetAllItemsQuery query) {
        return itemRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
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
