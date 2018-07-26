package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.embedded.WorkerPK;
import com.arcaneiceman.kraken.domain.enumerations.WorkerStatus;
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
@Table(name = "workers")
@Entity
public class Worker {
    /**
     * Composite PK because we query {@link Worker} through workerName and workerType
     */
    @EmbeddedId
    private WorkerPK id;

    @Column
    @Enumerated(EnumType.STRING)
    private WorkerStatus status;

    @Column
    private Date lastCheckIn;

    @OneToOne(fetch = FetchType.LAZY)
    private Job job;

}
