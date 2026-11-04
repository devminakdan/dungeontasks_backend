package com.dungeontasks.common.error;

import org.springframework.http.HttpStatus;

public class AuthenticationUnavailableException extends ApiException {

    public AuthenticationUnavailableException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Authentication unavailable", detail);
    }
}
