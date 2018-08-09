package com.arcaneiceman.kraken.domain.abs;

import lombok.*;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = false, of = "id")
@Setter
@Getter
@NoArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class RequestDetail {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
