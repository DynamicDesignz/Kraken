package com.wali.kraken.domain.core;

import com.wali.kraken.domain.CandidateValueList;
import com.wali.kraken.enumerations.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class CandidateValueListDescriptor {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueNumber;

    @Enumerated(EnumType.STRING)
    @Column
    private ProcessingStatus processingStatus;

    @ManyToOne
    @JoinColumn
    private CrackRequestDescriptor crackRequestDescriptor;

    @Column
    private String candidateValueListName;

}
