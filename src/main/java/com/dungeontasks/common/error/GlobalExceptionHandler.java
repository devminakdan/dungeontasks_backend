package com.dungeontasks.common.error;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(
            ApiException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                exception.getStatus(),
                exception.getTitle(),
                exception.getMessage(),
                request
        );
        return problemResponse(problem, exception.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more request fields are invalid",
                request
        );
        List<Map<String, String>> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::validationError)
                .toList();
        problem.setProperty("errors", errors);
        return problemResponse(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid request body",
                "The request body is missing or malformed",
                request
        );
        return problemResponse(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        if (exception instanceof ErrorResponse errorResponse) {
            ProblemDetail problem = errorResponse.getBody();
            problem.setInstance(URI.create(request.getRequestURI()));
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problem);
        }

        LOGGER.error("Unhandled API failure for {} {}", request.getMethod(), request.getRequestURI(), exception);
        ProblemDetail problem = createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "The server could not complete the request",
                request
        );
        return problemResponse(problem, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, String> validationError(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
        );
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private ResponseEntity<ProblemDetail> problemResponse(ProblemDetail problem, HttpStatus status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
