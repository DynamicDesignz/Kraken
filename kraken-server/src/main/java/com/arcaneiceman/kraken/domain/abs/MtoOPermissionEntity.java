package com.arcaneiceman.kraken.domain.abs;

/**
 * Created by Wali on 4/21/2018.
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Created by Wali on 05/03/18.
 */
@ToString(exclude = "owner")
@MappedSuperclass
public abstract class MtoOPermissionEntity<T> implements Serializable {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn
    private T owner;

    public T getOwner() {
        return owner;
    }

    public void setOwner(T owner) {
        this.owner = owner;
    }
}
