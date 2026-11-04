package com.dungeontasks.common.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, "Resource not found", detail);
    }
}
