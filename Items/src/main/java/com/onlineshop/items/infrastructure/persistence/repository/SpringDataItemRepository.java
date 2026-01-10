package com.onlineshop.items.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.onlineshop.items.infrastructure.persistence.entity.ItemJpaEntity;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for ItemJpaEntity.
 * This is an infrastructure concern, not exposed to domain layer.
 */
interface SpringDataItemRepository extends JpaRepository<ItemJpaEntity, UUID> {

    @Query("SELECT i FROM ItemJpaEntity i WHERE LOWER(i.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ItemJpaEntity> searchByDescription(@Param("searchTerm") String searchTerm);
}
