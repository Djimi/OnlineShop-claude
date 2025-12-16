package com.onlineshop.auth.repository;

public interface RefreshableRepository<T> {
    T saveAndRefresh(T entity);
}
