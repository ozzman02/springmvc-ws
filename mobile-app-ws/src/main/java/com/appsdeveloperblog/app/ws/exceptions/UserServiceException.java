package com.appsdeveloperblog.app.ws.exceptions;

public class UserServiceException extends RuntimeException {

    private static final long serialVersionUID = 180832930409211414L;

    public UserServiceException(String message) {
        super(message);
    }

}
