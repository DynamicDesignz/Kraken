package com.wali.kraken.domain.overwire;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;

@Data
public class Job implements Serializable {

    private long queueNumber;

    private long passwordListQueueNumber;

    private long passwordRequestQueueNumber;

    private String passwordCaptureInBase64;

    private ArrayList<String> candidateValues;

    private String charSet;
}
