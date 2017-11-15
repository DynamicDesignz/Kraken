package com.wali.kraken.domain.core;

import com.wali.kraken.domain.CandidateValueList;
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

    @Column
    private String processingStatus;

    @ManyToOne
    @JoinColumn(name = "request_queue_number")
    private CrackRequest crackRequest;

    @ManyToOne
    @JoinColumn(name = "candidate_value_list")
    private CandidateValueList candidateValueList;

}
