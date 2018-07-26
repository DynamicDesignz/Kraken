package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.embedded.JobDelimiter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.core.annotation.Order;

import javax.persistence.*;
import java.util.List;
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
@Table(name = "password_lists")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class PasswordList {

    @Id
    private String name;

    @Column
    private String charset;

    @JsonIgnore
    @ElementCollection
    @OrderBy("indexNumber")
    private List<JobDelimiter> jobDelimiterSet;
}
