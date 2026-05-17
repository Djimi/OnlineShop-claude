package com.onlineshop.items.web.controller;

import com.onlineshop.items.application.command.CreateItemCommand;
import com.onlineshop.items.application.command.DeleteItemCommand;
import com.onlineshop.items.application.command.UpdateItemCommand;
import com.onlineshop.items.application.dto.CreateItemResponse;
import com.onlineshop.items.application.dto.GetItemResponse;
import com.onlineshop.items.application.dto.UpdateItemResponse;
import com.onlineshop.items.application.query.GetAllItemsQuery;
import com.onlineshop.items.application.query.GetItemQuery;
import com.onlineshop.items.application.query.SearchItemsByDescriptionQuery;
import com.onlineshop.items.application.usecase.CreateItemUseCase;
import com.onlineshop.items.application.usecase.DeleteItemUseCase;
import com.onlineshop.items.application.usecase.GetAllItemsUseCase;
import com.onlineshop.items.application.usecase.GetItemUseCase;
import com.onlineshop.items.application.usecase.SearchItemsUseCase;
import com.onlineshop.items.application.usecase.UpdateItemUseCase;
import com.onlineshop.items.web.dto.CreateItemRequest;
import com.onlineshop.items.web.dto.ItemResponse;
import com.onlineshop.items.web.dto.UpdateItemRequest;
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
    private final DeleteItemUseCase deleteItemUseCase;
    private final GetItemUseCase getItemUseCase;
    private final GetAllItemsUseCase getAllItemsUseCase;
    private final SearchItemsUseCase searchItemsUseCase;

    public ItemsController(
            CreateItemUseCase createItemUseCase,
            UpdateItemUseCase updateItemUseCase,
            DeleteItemUseCase deleteItemUseCase,
            GetItemUseCase getItemUseCase,
            GetAllItemsUseCase getAllItemsUseCase,
            SearchItemsUseCase searchItemsUseCase) {
        this.createItemUseCase = createItemUseCase;
        this.updateItemUseCase = updateItemUseCase;
        this.deleteItemUseCase = deleteItemUseCase;
        this.getItemUseCase = getItemUseCase;
        this.getAllItemsUseCase = getAllItemsUseCase;
        this.searchItemsUseCase = searchItemsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<GetItemResponse> result = getAllItemsUseCase.execute(new GetAllItemsQuery());
        return ResponseEntity.ok(result.stream().map(this::toItemResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable UUID id) {
        GetItemResponse result = getItemUseCase.execute(new GetItemQuery(id));
        return ResponseEntity.ok(toItemResponse(result));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemResponse>> searchItems(@RequestParam String description) {
        List<GetItemResponse> result = searchItemsUseCase.execute(new SearchItemsByDescriptionQuery(description));
        return ResponseEntity.ok(result.stream().map(this::toItemResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@RequestBody CreateItemRequest request) {
        CreateItemResponse result = createItemUseCase.execute(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toItemResponse(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable UUID id, @RequestBody UpdateItemRequest request) {
        UpdateItemResponse result = updateItemUseCase.execute(toCommand(id, request));
        return ResponseEntity.ok(toItemResponse(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
        deleteItemUseCase.execute(new DeleteItemCommand(id));
        return ResponseEntity.noContent().build();
    }

    private CreateItemCommand toCommand(CreateItemRequest request) {
        return new CreateItemCommand(
                request.name(),
                request.quantity(),
                request.description()
        );
    }

    private UpdateItemCommand toCommand(UUID id, UpdateItemRequest request) {
        return new UpdateItemCommand(
                id,
                request.name(),
                request.quantity(),
                request.description()
        );
    }

    private ItemResponse toItemResponse(GetItemResponse dto) {
        return new ItemResponse(dto.id(), dto.name(), dto.quantity(), dto.description());
    }

    private ItemResponse toItemResponse(CreateItemResponse dto) {
        return new ItemResponse(dto.id(), dto.name(), dto.quantity(), dto.description());
    }

    private ItemResponse toItemResponse(UpdateItemResponse dto) {
        return new ItemResponse(dto.id(), dto.name(), dto.quantity(), dto.description());
    }
}
