package com.arcaneiceman.kraken.domain.embedded;

import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Transient;
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
public class Job implements Serializable {

    private Integer indexNumber;

    private TrackingStatus trackingStatus;

    private String start;

    private String end;

    private Integer errorCount;

    private Date timeoutAt;

    @Transient
    private List<String> values;

}
