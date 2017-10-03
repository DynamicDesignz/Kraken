package com.wali.kraken.domain;

import lombok.Data;

import java.util.UUID;

@Data
public class JobDescriptor {

    public UUID ID;

    public String requestID;

    public long start;

    public long end;

    public long timeRunning;
}
