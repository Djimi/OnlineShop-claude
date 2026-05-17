package com.onlineshop.items.application.dto.mapper;

import com.onlineshop.items.application.dto.CreateItemResponse;
import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.dto.UpdateItemResponse;
import com.onlineshop.items.domain.aggregateroots.Item;
import org.springframework.stereotype.Component;

@Component
public class ItemResponseMapper {

    public GetItemResponse toGetItemResponse(Item item) {
        return new GetItemResponse(
                item.getId() != null ? item.getId().getValue() : null,
                item.getName().value(),
                item.getQuantity().amount(),
                item.getDescription() != null ? item.getDescription().value() : null
        );
    }

    public CreateItemResponse toCreateItemResponse(Item item) {
        return new CreateItemResponse(
                item.getId() != null ? item.getId().getValue() : null,
                item.getName().value(),
                item.getQuantity().amount(),
                item.getDescription() != null ? item.getDescription().value() : null
        );
    }

    public UpdateItemResponse toUpdateItemResponse(Item item) {
        return new UpdateItemResponse(
                item.getId() != null ? item.getId().getValue() : null,
                item.getName().value(),
                item.getQuantity().amount(),
                item.getDescription() != null ? item.getDescription().value() : null
        );
    }
}
