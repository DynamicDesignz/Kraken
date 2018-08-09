package com.arcaneiceman.kraken.domain.embedded;

import lombok.*;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@Embeddable
public class CrunchListJob implements Serializable {

    private Integer indexNumber;

    private String startString;

    private String endString;

    private Date timeoutAt;
}
