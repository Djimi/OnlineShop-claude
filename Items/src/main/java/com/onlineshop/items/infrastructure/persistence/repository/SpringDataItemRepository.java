package com.onlineshop.items.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onlineshop.items.infrastructure.persistence.entity.ItemJpaEntity;

/**
 * Spring Data JPA repository for ItemJpaEntity.
 * This is an infrastructure concern, not exposed to domain layer.
 */
interface SpringDataItemRepository extends JpaRepository<ItemJpaEntity, Long> {
}
