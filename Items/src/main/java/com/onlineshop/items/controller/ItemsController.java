package com.onlineshop.items.controller;

import com.onlineshop.items.dto.ItemDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemsController {

    @GetMapping
    public List<ItemDTO> getAllItems() {
        return List.of(
            new ItemDTO(1L, "Laptop", "In Stock"),
            new ItemDTO(2L, "Mouse", "In Stock"),
            new ItemDTO(3L, "Keyboard", "Out of Stock"),
            new ItemDTO(4L, "Monitor", "In Stock"),
            new ItemDTO(5L, "Headphones", "Limited Stock")
        );
    }
}
