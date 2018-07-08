package com.arcaneiceman.kraken.domain.embedded;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@Embeddable
public class JobDelimter implements Serializable {

    @Column
    private Long startByte;

    @Column
    private Long endByte;
}
