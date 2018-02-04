package com.wali.kraken.domain.core;

import com.wali.kraken.domain.enumerations.ProcessingStatus;
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
public class JobDescriptor {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueNumber;

    @Enumerated(EnumType.STRING)
    @Column
    private ProcessingStatus processingStatus;

    @ManyToOne
    @JoinColumn
    private CandidateValueListDescriptor candidateValueListDescriptor;

    @Column
    @Lob
    private String colonDelimitedCandidateValues;

    @Column
    public long startLine;

    @Column
    public long endLine;

    @Column
    public long timeRunning;

    @Column
    public int attempts;
}
