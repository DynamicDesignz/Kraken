package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.enumerations.WorkerStatus;
import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "worker")
@Entity
public class Worker extends MtoOPermissionEntity<User> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private WorkerStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private WorkerType workerType;

    @Column
    private String workerName;

    @Column
    private Integer benchmark;

    @Column
    private Date lastCheckIn;

    @Column
    private String location;
}
