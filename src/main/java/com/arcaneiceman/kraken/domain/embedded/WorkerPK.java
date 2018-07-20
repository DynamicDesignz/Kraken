package com.arcaneiceman.kraken.domain.embedded;

import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class WorkerPK implements Serializable {

    private String workerName;

    private WorkerType workerType;

    private Long userId;

}
