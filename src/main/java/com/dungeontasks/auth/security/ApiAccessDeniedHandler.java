package com.dungeontasks.auth.security;

import java.io.IOException;

import com.dungeontasks.common.error.ProblemResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemResponseWriter problemWriter;

    public ApiAccessDeniedHandler(ProblemResponseWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        problemWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "The authenticated user cannot access this resource"
        );
    }
}
