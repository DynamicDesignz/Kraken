package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.TrackedList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TrackedCrunchList extends TrackedList {

    @Column
    private Integer minSize;

    @Column
    private Integer maxSize;

    @Column
    private String characters;

    @Column
    private String nextJobString;


}
