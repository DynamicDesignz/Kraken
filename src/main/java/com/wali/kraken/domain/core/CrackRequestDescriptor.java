package com.wali.kraken.domain.core;

import com.wali.kraken.domain.enumerations.ProcessingStatus;
import com.wali.kraken.domain.enumerations.RequestType;
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
public class CrackRequestDescriptor {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueNumber;

    @Enumerated(EnumType.STRING)
    @Column
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column
    private ProcessingStatus processingStatus;

    @Column
    @Lob
    private String passwordCaptureInBase64;

    @Column
    private String colonDelimitedCandidateValueListNames;

    /* This field contains values relevant to the Request Type
    *
    *   Eg : WPA Crack Requests would require SSID as Map data
    */
    @Column
    private String metadataMap;

    @Column
    private String result;

}
