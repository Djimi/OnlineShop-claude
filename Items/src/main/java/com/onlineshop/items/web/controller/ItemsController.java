package com.onlineshop.items.web.controller;

import com.onlineshop.items.application.command.CreateItemCommand;
import com.onlineshop.items.application.command.UpdateItemCommand;
import com.onlineshop.items.application.dto.CreateItemResponse;
import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.dto.UpdateItemResponse;
import com.onlineshop.items.application.query.GetAllItemsQuery;
import com.onlineshop.items.application.query.GetItemQuery;
import com.onlineshop.items.application.query.SearchItemsByDescriptionQuery;
import com.onlineshop.items.application.usecase.CreateItemUseCase;
import com.onlineshop.items.application.usecase.GetAllItemsUseCase;
import com.onlineshop.items.application.usecase.GetItemUseCase;
import com.onlineshop.items.application.usecase.SearchItemsUseCase;
import com.onlineshop.items.application.usecase.UpdateItemUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items")
public class ItemsController {

    private final CreateItemUseCase createItemUseCase;
    private final UpdateItemUseCase updateItemUseCase;
    private final GetItemUseCase getItemUseCase;
    private final GetAllItemsUseCase getAllItemsUseCase;
    private final SearchItemsUseCase searchItemsUseCase;

    public ItemsController(
            CreateItemUseCase createItemUseCase,
            UpdateItemUseCase updateItemUseCase,
            GetItemUseCase getItemUseCase,
            GetAllItemsUseCase getAllItemsUseCase,
            SearchItemsUseCase searchItemsUseCase) {
        this.createItemUseCase = createItemUseCase;
        this.updateItemUseCase = updateItemUseCase;
        this.getItemUseCase = getItemUseCase;
        this.getAllItemsUseCase = getAllItemsUseCase;
        this.searchItemsUseCase = searchItemsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<GetItemResponse>> getAllItems() {
        return ResponseEntity.ok(getAllItemsUseCase.execute(new GetAllItemsQuery()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetItemResponse> getItemById(@PathVariable UUID id) {
        return ResponseEntity.ok(getItemUseCase.execute(new GetItemQuery(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<GetItemResponse>> searchItems(@RequestParam String description) {
        return ResponseEntity.ok(searchItemsUseCase.execute(new SearchItemsByDescriptionQuery(description)));
    }

    @PostMapping
    public ResponseEntity<CreateItemResponse> createItem(@RequestBody CreateItemCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createItemUseCase.execute(command));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateItemResponse> updateItem(@PathVariable UUID id, @RequestBody UpdateItemCommand command) {
        return ResponseEntity.ok(updateItemUseCase.execute(command));
    }
}
