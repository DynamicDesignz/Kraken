package com.arcaneiceman.kraken.controller.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Wali on 4/1/2018.
 */
public class ActiveRequestIO {
    public static class GetJob {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {

            String jobId;

            List<String> candidateValues;

            String passwordCaptureFileLink;
        }

    }
}
