package com.onlineshop.items.application.usecase;

import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.dto.mapper.ItemResponseMapper;
import com.onlineshop.items.application.query.SearchItemsByDescriptionQuery;
import com.onlineshop.items.domain.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchItemsUseCase {

    private final ItemRepository itemRepository;
    private final ItemResponseMapper mapper;

    public SearchItemsUseCase(ItemRepository itemRepository, ItemResponseMapper mapper) {
        this.itemRepository = itemRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<GetItemResponse> execute(SearchItemsByDescriptionQuery query) {
        return itemRepository.searchByDescription(query.searchTerm()).stream()
            .map(mapper::toGetItemResponse)
            .toList();
    }
}
