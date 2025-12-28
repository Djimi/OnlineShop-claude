package com.onlineshop.items.infrastructure.persistence.repository;

import com.onlineshop.common.domain.valueobject.ItemId;
import com.onlineshop.items.domain.Item;
import com.onlineshop.items.domain.repository.ItemRepository;
import com.onlineshop.items.infrastructure.persistence.entity.ItemJpaEntity;
import com.onlineshop.items.infrastructure.persistence.entity.ItemMapper;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing the domain ItemRepository interface.
 * This bridges the domain layer with the infrastructure persistence layer.
 */
@Repository
class JpaItemRepositoryAdapter implements ItemRepository {

    private final SpringDataItemRepository jpaRepository;
    private final ItemMapper mapper;

    JpaItemRepositoryAdapter(SpringDataItemRepository jpaRepository, ItemMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Item save(Item item) {
        ItemJpaEntity entity = mapper.toEntity(item);
        ItemJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Item> findById(ItemId id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public List<Item> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public void delete(Item item) {
        if (item.getId() != null) {
            jpaRepository.deleteById(item.getId().getValue());
        }
    }

    @Override
    public boolean existsById(ItemId id) {
        return jpaRepository.existsById(id.getValue());
    }
}
