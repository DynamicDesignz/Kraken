package com.arcaneiceman.kraken.domain;

import com.arcaneiceman.kraken.domain.abs.MtoOPermissionEntity;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = false, of = "id")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "results")
@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Result extends MtoOPermissionEntity<User> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private RequestType requestType;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn
    private RequestDetail requestDetail;

    @Column
    private String value;

    @Column
    private Integer totalJobCount;

    @Column
    private Integer errorJobCount;

    @Column
    private Integer completedJobCount;

}
