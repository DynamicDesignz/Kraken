package com.arcaneiceman.kraken.domain.embedded;

import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class WorkerPK implements Serializable {

    private String workerName;

    @Enumerated(EnumType.STRING)
    private WorkerType workerType;

    private Long userId;

}

