package com.arcaneiceman.kraken.util.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class KrakenException extends RuntimeException {

    private HttpStatus httpStatus;

    public KrakenException(HttpStatus status, String message) {
        super(message);
        this.httpStatus = status;
    }

}
