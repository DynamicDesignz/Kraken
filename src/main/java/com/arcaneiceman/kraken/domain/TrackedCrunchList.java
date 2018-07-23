package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.embedded.Job;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "id")
@Entity
@Table(name = "tracked_crunch_list")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TrackedCrunchList extends MtoOPermissionEntity<Request> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private Integer minSize;

    @Column
    private Integer maxSize;

    @Column
    private String characters;

    @Column
    @Enumerated(EnumType.STRING)
    private TrackingStatus status;

    @Column
    private Integer totalJobs;

    @Column
    private String nextJobString;

    @Column
    private Integer nextJobIndex;

    @Column
    private Integer completedJobCount;

    @Column
    private Integer errorJobCount;

    @JsonIgnore
    @Embedded
    @OrderBy("indexNumber")
    private List<Job> jobQueue;

}
