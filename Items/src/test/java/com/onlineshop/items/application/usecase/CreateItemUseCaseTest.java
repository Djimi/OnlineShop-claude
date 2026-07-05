package com.onlineshop.items.application.usecase;

import com.onlineshop.common.domain.valueobject.ItemDescription;
import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.common.domain.valueobject.ItemName;
import com.onlineshop.common.domain.valueobject.Quantity;
import com.onlineshop.items.application.command.CreateItemCommand;
import com.onlineshop.items.application.dto.CreateItemResponse;
import com.onlineshop.items.domain.aggregateroots.Item;
import com.onlineshop.items.domain.event.ItemCreated;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.domain.service.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateItemUseCaseTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private CreateItemUseCase createItemUseCase;

    @Test
    void execute_whenValidCommand_createsItemAndPublishesEvent() {
        UUID generatedId = UUID.randomUUID();
        when(idGenerator.generate()).thenReturn(generatedId);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateItemCommand command = new CreateItemCommand("New Item", 5, "A new item");
        CreateItemResponse response = createItemUseCase.execute(command);

        assertThat(response.id()).isEqualTo(generatedId);
        assertThat(response.name()).isEqualTo("New Item");
        assertThat(response.quantity()).isEqualTo(5);
        assertThat(response.description()).isEqualTo("A new item");

        verify(itemRepository).save(any(Item.class));
        verify(eventPublisher).publishEvent(any(ItemCreated.class));
    }

    @Test
    void execute_whenValidCommandWithNullDescription_returnsNullDescription() {
        UUID generatedId = UUID.randomUUID();
        when(idGenerator.generate()).thenReturn(generatedId);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateItemCommand command = new CreateItemCommand("No Desc Item", 3, null);
        CreateItemResponse response = createItemUseCase.execute(command);

        assertThat(response.id()).isEqualTo(generatedId);
        assertThat(response.name()).isEqualTo("No Desc Item");
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.description()).isNull();

        verify(eventPublisher).publishEvent(any(ItemCreated.class));
    }

    @Test
    void execute_whenValidCommand_zeroQuantity() {
        UUID generatedId = UUID.randomUUID();
        when(idGenerator.generate()).thenReturn(generatedId);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateItemCommand command = new CreateItemCommand("Zero Qty Item", 0, "Item with zero quantity");
        CreateItemResponse response = createItemUseCase.execute(command);

        assertThat(response.quantity()).isZero();
        verify(eventPublisher).publishEvent(any(ItemCreated.class));
    }
}
