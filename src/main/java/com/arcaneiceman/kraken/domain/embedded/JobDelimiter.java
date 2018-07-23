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
public class JobDelimiter implements Serializable {

    private Integer indexNumber;

    private Long startByte;

    private Long endByte;
}
