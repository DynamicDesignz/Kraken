package com.arcaneiceman.kraken.service.permission.abs;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.security.access.AccessDeniedException;
import org.zalando.problem.AbstractThrowableProblem;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Wali on 07/03/18.
 */
public abstract class MtoOPermissionLayer<T extends MtoOPermissionEntity, ID extends Serializable> {

    private PagingAndSortingRepository<T, ID> pagingAndSortingRepository;

    public MtoOPermissionLayer(PagingAndSortingRepository<T, ID> pagingAndSortingRepository) {
        this.pagingAndSortingRepository = pagingAndSortingRepository;
    }

    public abstract AbstractThrowableProblem getNotFoundThrowable(ID id);

    public T getWithOwner(ID id, Object o) {
        T entity = get(id);
        Object owner = entity.getOwner();
        if (!Objects.equals(o, owner))
            throw new AccessDeniedException("error.http.403");
        return entity;
    }

    public T get(ID id) {
        return pagingAndSortingRepository.findById(id)
                .orElseThrow(() -> {
                    if (getNotFoundThrowable(id) == null)
                        return new RuntimeException("Entity with id " + id + " not found");
                    else
                        return getNotFoundThrowable(id);
                });
    }
}
