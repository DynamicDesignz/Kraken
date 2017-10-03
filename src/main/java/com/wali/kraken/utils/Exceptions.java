package com.wali.kraken.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class Exceptions {

    @ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
    public static class InvalidPasswordFileException extends RuntimeException {
        public InvalidPasswordFileException() {
            super();
        }
        public InvalidPasswordFileException(String message, Throwable cause) {
            super(message, cause);
        }
        public InvalidPasswordFileException(String message) {
            super(message);
        }
        public InvalidPasswordFileException(Throwable cause) {
            super(cause);
        }
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public static class PasswordRequestNotFound extends RuntimeException {
        public PasswordRequestNotFound() {
            super();
        }
        public PasswordRequestNotFound(String message, Throwable cause) {
            super(message, cause);
        }
        public PasswordRequestNotFound(String message) {
            super(message);
        }
        public PasswordRequestNotFound(Throwable cause) {
            super(cause);
        }
    }


}
