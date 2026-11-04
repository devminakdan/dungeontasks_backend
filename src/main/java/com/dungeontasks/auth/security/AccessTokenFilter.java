package com.dungeontasks.auth.security;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.dungeontasks.auth.domain.AuthSession;
import com.dungeontasks.auth.repository.AuthSessionRepository;
import com.dungeontasks.common.error.InvalidAuthenticationException;
import com.dungeontasks.common.error.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final AuthSessionRepository sessionRepository;
    private final ProblemResponseWriter problemWriter;
    private final Clock clock;

    public AccessTokenFilter(
            TokenService tokenService,
            AuthSessionRepository sessionRepository,
            ProblemResponseWriter problemWriter,
            Clock clock
    ) {
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.problemWriter = problemWriter;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX) || authorization.length() == BEARER_PREFIX.length()) {
            writeUnauthorized(request, response);
            return;
        }

        try {
            Jwt jwt = tokenService.decodeAccessToken(authorization.substring(BEARER_PREFIX.length()));
            UUID userID = tokenService.userID(jwt);
            UUID sessionID = tokenService.sessionID(jwt);
            AuthSession session = sessionRepository.findByID(sessionID)
                    .filter(candidate -> candidate.userID().equals(userID))
                    .filter(candidate -> candidate.isActiveAt(clock.instant()))
                    .orElseThrow(() -> new InvalidAuthenticationException("The session is invalid or expired"));
            CurrentUser principal = new CurrentUser(session.userID(), session.sessionID());
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    jwt,
                    List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidAuthenticationException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response);
        }
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        problemWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "The access token is invalid, expired, or revoked"
        );
    }
}
