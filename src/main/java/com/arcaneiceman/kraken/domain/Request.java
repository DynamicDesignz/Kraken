package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = false, of = "id")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "request")
@Entity
public class Request extends MtoOPermissionEntity<User> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private RequestType requestType;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn
    private RequestDetail requestDetail;

    @JsonIgnore
    @OrderBy("id")
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackedPasswordList> trackedPasswordLists;

    @JsonIgnore
    @OrderBy("id")
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade =  CascadeType.ALL, orphanRemoval = true)
    private List<TrackedCrunchList> trackedCrunchLists;

}
