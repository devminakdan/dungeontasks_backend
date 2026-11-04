package com.dungeontasks.auth.security;

import java.io.IOException;

import com.dungeontasks.common.error.ProblemResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemResponseWriter problemWriter;

    public ApiAuthenticationEntryPoint(ProblemResponseWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        problemWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "A valid access token is required"
        );
    }
}
