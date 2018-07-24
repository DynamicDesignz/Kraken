package com.arcaneiceman.kraken.domain.abs;

import com.arcaneiceman.kraken.domain.Request;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "id")
@MappedSuperclass
public abstract class TrackedList extends MtoOPermissionEntity<Request> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

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

}
