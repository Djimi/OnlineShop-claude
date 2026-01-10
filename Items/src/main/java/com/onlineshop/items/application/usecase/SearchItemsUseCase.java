package com.onlineshop.items.application.usecase;

import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.query.SearchItemsByDescriptionQuery;
import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case for searching items by description.
 */
@Service
public class SearchItemsUseCase {

    private final ItemRepository itemRepository;

    public SearchItemsUseCase(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<GetItemResponse> execute(SearchItemsByDescriptionQuery query) {
        return itemRepository.searchByDescription(query.searchTerm()).stream()
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
