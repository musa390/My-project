package com.luxeperfume.backend.common.repository;

import com.luxeperfume.backend.common.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * Shared Spring Data repository contract for persisted domain entities.
 * Domain repositories should extend this interface.
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID extends Serializable>
        extends JpaRepository<T, ID> {
}
