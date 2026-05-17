package com.onlineshop.items.application.usecase;

import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.dto.mapper.ItemResponseMapper;
import com.onlineshop.items.application.query.GetItemQuery;
import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.exception.ItemNotFoundException;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.domain.valueobject.ItemId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetItemUseCase {

    private final ItemRepository itemRepository;
    private final ItemResponseMapper mapper;

    public GetItemUseCase(ItemRepository itemRepository, ItemResponseMapper mapper) {
        this.itemRepository = itemRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public GetItemResponse execute(GetItemQuery query) {
        ItemId itemId = new ItemId(query.id());
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException(query.id()));

        return mapper.toGetItemResponse(item);
    }
}
