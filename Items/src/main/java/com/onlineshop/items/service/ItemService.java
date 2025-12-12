package com.onlineshop.items.service;

import com.onlineshop.items.dto.ItemDTO;
import com.onlineshop.items.entity.Item;
import com.onlineshop.items.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<ItemDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ItemDTO convertToDTO(Item item) {
        return new ItemDTO(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getDescription()
        );
    }
}
