package com.luxeperfume.backend.common.service;

import com.luxeperfume.backend.common.entity.BaseEntity;
import com.luxeperfume.backend.common.exception.ResourceNotFoundException;
import com.luxeperfume.backend.common.repository.BaseRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * Reusable transactional CRUD implementation.
 * Concrete domain services supply their own repository and business rules.
 */
@Transactional(readOnly = true)
/** [INH] Reusable transactional CRUD base for BaseEntity-backed repositories. */
public abstract class AbstractCrudService<
        T extends BaseEntity,
        ID extends Serializable,
        R extends BaseRepository<T, ID>> implements CrudService<T, ID> {

    protected final R repository;

    protected AbstractCrudService(R repository) {
        this.repository = repository;
    }

    @Override
    public T getById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requested resource was not found"));
    }

    @Override
    public List<T> getAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public T save(T entity) {
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        T entity = getById(id);
        repository.delete(entity);
    }
}
