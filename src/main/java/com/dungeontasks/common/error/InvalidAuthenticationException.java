package com.dungeontasks.common.error;

import org.springframework.http.HttpStatus;

public class InvalidAuthenticationException extends ApiException {

    public InvalidAuthenticationException(String detail) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
    }
}
