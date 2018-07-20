package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "id")
@Entity
@Table(name = "tracked_password_list")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TrackedCrunchList {

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
    private Long totalJobs;

    @Column
    private Long nextJobNumber;

    @Column
    private String lastJobPasswordValue;

    @Column
    private Long completedJobNumber;

    @Column
    private Long runningJobNumber;

    @Column
    private Long errorJobNumber;

}
