package com.luxeperfume.backend.common.service;

import com.luxeperfume.backend.common.entity.BaseEntity;

import java.io.Serializable;
import java.util.List;

/**
 * Common CRUD service contract for domain services.
 */
/** [CRUD] Shared CRUD contract; domain services must add authorization and business invariants. */
public interface CrudService<T extends BaseEntity, ID extends Serializable> {
    T getById(ID id);
    List<T> getAll();
    T save(T entity);
    void deleteById(ID id);
}
