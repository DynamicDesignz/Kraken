package com.arcaneiceman.kraken.controller.io;

import com.arcaneiceman.kraken.domain.WPARequestDetail;
import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by Wali on 4/1/2018.
 */
public class RequestIO {

    public static class Create {

        @Getter
        @NoArgsConstructor
        public static class Request {

            @NotNull
            private RequestType requestType;

            @NotNull
            @JsonTypeInfo(
                    use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = "requestType"
            )
            @JsonSubTypes({
                    @JsonSubTypes.Type(value = WPARequestDetail.class, name = "WPA"),
            })
            private RequestDetail requestDetail;
        }
    }

    public static class GetJob {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {

            RequestType requestType;

            RequestDetail requestDetail;

            String jobId;

            List<String> candidateValues;
        }

    }
}
