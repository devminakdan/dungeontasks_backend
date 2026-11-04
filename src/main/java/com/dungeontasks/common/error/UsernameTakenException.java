package com.dungeontasks.common.error;

import org.springframework.http.HttpStatus;

public class UsernameTakenException extends ApiException {

    public UsernameTakenException() {
        super(HttpStatus.CONFLICT, "Username unavailable", "The requested username is already in use");
    }
}
