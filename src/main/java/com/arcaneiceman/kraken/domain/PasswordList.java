package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.embedded.JobDelimter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Set;

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
public class PasswordList {

    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private String charset;

    @JsonIgnore
    @ElementCollection
    private Set<JobDelimter> jobDelimiterSet;
}
