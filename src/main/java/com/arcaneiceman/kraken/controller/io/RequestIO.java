package com.arcaneiceman.kraken.controller.io;

import com.arcaneiceman.kraken.domain.abs.RequestDetail;
import com.arcaneiceman.kraken.domain.enumerations.RequestType;
import com.arcaneiceman.kraken.domain.enumerations.TrackingStatus;
import com.arcaneiceman.kraken.domain.request.detail.MatchRequestDetail;
import com.arcaneiceman.kraken.domain.request.detail.WPARequestDetail;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by Wali on 4/1/2018.
 */
public class RequestIO {

    public static class Create {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
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
                    @JsonSubTypes.Type(value = MatchRequestDetail.class, name = "MATCH")
            })
            private RequestDetail requestDetail;

            private List<String> passwordLists;

            private List<CrunchParams> crunchParams;

            @Getter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class CrunchParams {

                @NotNull
                private Integer minSize;

                @NotNull
                private Integer maxSize;

                @NotEmpty
                private String characters;

                @NotEmpty
                private String startString;
            }
        }
    }

    public static class GetJob {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Response {

            RequestType requestType;

            RequestDetail requestDetail;

            Long requestId;

            Long listId;

            String jobId;

            List<String> candidateValues;
        }

    }

    public static class ReportJob {

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {

            @NotNull
            Long requestId;

            @NotNull
            Long listId;

            @NotNull
            String jobId;

            @NotNull
            TrackingStatus trackingStatus;

            String result;
        }
    }

}
