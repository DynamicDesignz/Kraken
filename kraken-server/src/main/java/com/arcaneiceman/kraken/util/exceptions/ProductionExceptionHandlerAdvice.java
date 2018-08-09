package com.arcaneiceman.kraken.util.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Created by wali on 16/10/17.
 */
@Profile({"prod", "production"})
@ControllerAdvice
public class ProductionExceptionHandlerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ProductionExceptionHandlerAdvice.class);

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ExceptionDTO> handleException(SystemException e) {
        log.error("Error Code {}, Error Message : {}", e.getErrorCode(), e.getMessage());
        return new ResponseEntity<>(new ExceptionDTO(e.getErrorCode()), HttpStatus.valueOf(e.getStatus().getStatusCode()));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private class ExceptionDTO {
        private int errorCode;
    }
}
