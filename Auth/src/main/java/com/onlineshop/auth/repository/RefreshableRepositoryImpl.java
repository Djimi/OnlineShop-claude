package com.onlineshop.auth.repository;

import com.onlineshop.auth.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshableRepositoryImpl<T> implements RefreshableRepository<User> {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    @Override
    public User saveAndRefresh(User entity) {
        User savedSession = userRepository.saveAndFlush(entity);
        entityManager.refresh(savedSession);
        return savedSession;
    }
}
