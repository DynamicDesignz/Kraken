package com.arcaneiceman.kraken.domain;

import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.nio.charset.Charset;

/**
 * Created by Wali on 4/21/2018.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(name = "candidate_value_list")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CandidateValueList {

    @Id
    private String name;

    @Column
    private String charset;
}
