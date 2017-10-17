package com.wali.kraken.domain.core;

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
public class CrackRequest {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueNumber;

    @Column
    private String requestType;

    /**
     * Processing Status Definition {@link ProcessingStatus}
     * <p>
     * {@link ProcessingStatus#ERROR} : Failed to launch request when processing was beginning
     * <p>
     * {@link ProcessingStatus#PENDING} : Currently pending to be processed
     */
    @Column
    private String processingStatus;

    @Column
    private String SSIDToFind;

    @Column
    private String passwordCaptureInBase64;

    @Column
    private String colonDelimitedPasswordListNames;

    @Column
    private String result;

}
