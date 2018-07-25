package com.arcaneiceman.kraken.domain.abs;

import com.arcaneiceman.kraken.domain.Job;
import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.enumerations.ListType;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "id")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class TrackedList extends MtoOPermissionEntity<Request> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    @Enumerated
    private ListType listType;

    @Column
    @Enumerated(EnumType.STRING)
    private TrackingStatus status;

    @Column
    private Integer totalJobCount;

    @Column
    private Integer nextJobIndex;

    @Column
    private Integer completedJobCount;

    @Column
    private Integer errorJobCount;

    @JsonIgnore
    @OrderBy("indexNumber")
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Job> jobQueue;

}
