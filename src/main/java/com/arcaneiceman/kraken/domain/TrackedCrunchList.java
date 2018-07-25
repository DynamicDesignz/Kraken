package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "tracked_crunch_list")
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
