package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.TrackedList;
import com.arcaneiceman.kraken.domain.embedded.Job;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "tracked_password_list")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TrackedPasswordList extends TrackedList {

    @Column
    private String passwordListName;

    @JsonIgnore
    @Embedded
    @OrderBy("indexNumber")
    private List<Job> jobQueue;
}
