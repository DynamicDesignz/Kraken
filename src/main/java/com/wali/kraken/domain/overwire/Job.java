package com.wali.kraken.domain.overwire;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.ArrayList;

@Data
@AllArgsConstructor
public class Job implements Serializable {

    private long queueNumber;

    private long candidateValueListDescriptorQueueNumber;

    private long crackRequestQueueNumber;

    private String toMatchValueInBase64;

    private String metadataMap;

    private String colonDelimitedCandidateValues;

    private String charSet;
}
