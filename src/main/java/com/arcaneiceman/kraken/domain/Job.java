package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by Wali on 4/21/2018.
 */
@EqualsAndHashCode(callSuper = false, of = "indexNumber")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Table(name = "jobs")
@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Job extends MtoOPermissionEntity<TrackedList>{

    private static final long serialVersionUID = 1L;

    /**
     * Using synthetic key to avoid repeating data
     */
    @Id
    private String id;

    @Column
    private Integer indexNumber;

    @Column
    private TrackingStatus trackingStatus;

    @Column
    private String start;

    @Column
    private String end;

    @Column
    private Integer errorCount;

    @Column
    private Date timeoutAt;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    private Worker worker;

    @Transient
    private List<String> values;

}
