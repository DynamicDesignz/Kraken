package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.enumerations.TrackedJobStatus;
import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by Wali on 4/21/2018.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "id")
@Entity
@Table(name = "tracked_job")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TrackedJob extends MtoOPermissionEntity<Request> {

    @Id
    private String id;

    @Column
    private String candidateValueListName;

    @Column
    private String candidateValueListCharset;

    @Column
    private Long startByte;

    @Column
    private Long endByte;

    @Column
    @Enumerated(EnumType.STRING)
    private TrackedJobStatus status;

}
