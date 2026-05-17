package com.onlineshop.items.application.usecase;

import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.dto.mapper.ItemResponseMapper;
import com.onlineshop.items.application.query.GetAllItemsQuery;
import com.onlineshop.items.domain.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetAllItemsUseCase {

    private final ItemRepository itemRepository;
    private final ItemResponseMapper mapper;

    public GetAllItemsUseCase(ItemRepository itemRepository, ItemResponseMapper mapper) {
        this.itemRepository = itemRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<GetItemResponse> execute(GetAllItemsQuery query) {
        return itemRepository.findAll().stream()
            .map(mapper::toGetItemResponse)
            .toList();
    }
}
