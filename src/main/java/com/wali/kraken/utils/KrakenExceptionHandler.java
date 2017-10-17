package com.wali.kraken.utils;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Created by Wali on 10/16/2017.
 */
@ControllerAdvice
public class KrakenExceptionHandler {

    @ExceptionHandler(KrakenException.class)
    public ResponseEntity<String> throwException(KrakenException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getHttpStatus());
    }
}