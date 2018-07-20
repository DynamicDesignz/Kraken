package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "id")
@Entity
@Table(name = "tracked_password_list")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TrackedPasswordList extends MtoOPermissionEntity<Request> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String candidateValueListName;

    @Column
    private String candidateValueListCharset;

    @Column
    @Enumerated(EnumType.STRING)
    private TrackingStatus status;

    @JsonIgnore
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<TrackedPasswordListJob> trackedPasswordListJobs;
}
