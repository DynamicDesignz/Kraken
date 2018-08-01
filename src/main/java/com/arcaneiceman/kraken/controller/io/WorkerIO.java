package com.arcaneiceman.kraken.controller.io;

import com.arcaneiceman.kraken.domain.enumerations.WorkerType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class WorkerIO {

    public static class Augment {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {

            private String token;
        }

    }

    public static class Create {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {

            @NotNull
            private WorkerType workerType;

            @NotBlank
            private String workerName;
        }
    }
}
