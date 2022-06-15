package com.appsdeveloperblog.app.ws.exceptions;

import org.springframework.http.HttpStatus;

public class UserServiceException extends RuntimeException {

    private static final long serialVersionUID = 180832930409211414L;

    private final HttpStatus httpStatus;

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public UserServiceException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

}
