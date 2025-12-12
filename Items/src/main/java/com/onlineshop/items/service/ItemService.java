package com.onlineshop.items.service;

import com.onlineshop.items.dto.ItemDTO;
import com.onlineshop.items.entity.Item;
import com.onlineshop.items.exception.ItemNotFoundException;
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

    @Transactional(readOnly = true)
    public ItemDTO getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
        return convertToDTO(item);
    }

    @Transactional
    public ItemDTO createItem(ItemDTO itemDTO) {
        Item item = new Item(
                null,  // ID will be auto-generated
                itemDTO.getName(),
                itemDTO.getQuantity(),
                itemDTO.getDescription()
        );
        Item savedItem = itemRepository.save(item);
        return convertToDTO(savedItem);
    }

    @Transactional
    public ItemDTO updateItem(Long id, ItemDTO itemDTO) {
        Item existingItem = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));

        existingItem.setName(itemDTO.getName());
        existingItem.setQuantity(itemDTO.getQuantity());
        existingItem.setDescription(itemDTO.getDescription());

        Item updatedItem = itemRepository.save(existingItem);
        return convertToDTO(updatedItem);
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
