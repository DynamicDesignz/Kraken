package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import lombok.*;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by Wali on 4/21/2018.
 */
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "tracked_password_list_job")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TrackedPasswordListJob extends MtoOPermissionEntity<TrackedPasswordList> {

    @Id
    private String id;

    @Column
    private Long startByte;

    @Column
    private Long endByte;

    @Column
    @Enumerated(EnumType.STRING)
    private TrackingStatus status;

}
