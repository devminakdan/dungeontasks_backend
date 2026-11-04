package com.dungeontasks.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthProfileIntegrationTest {

    private static final String GOOGLE_AUDIENCE = "phase2-test.apps.googleusercontent.com";
    private static final String TEST_JWT_SECRET = "phase2-test-secret-with-at-least-32-bytes";
    private static final RSAKey GOOGLE_SIGNING_KEY = createSigningKey();
    private static final HttpServer JWKS_SERVER = startJwksServer();

    @LocalServerPort
    private int serverPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> environment("TEST_DB_URL",
                "jdbc:postgresql://localhost:55432/dungeontasks"));
        registry.add("spring.datasource.username", () -> environment("TEST_DB_USERNAME", "dungeontasks"));
        registry.add("spring.datasource.password", () -> environment("TEST_DB_PASSWORD", "phase2-test-db"));
        registry.add("app.auth.jwt-secret", () -> TEST_JWT_SECRET);
        registry.add("app.auth.google-client-id", () -> GOOGLE_AUDIENCE);
        registry.add("app.auth.google-jwk-set-uri",
                () -> "http://localhost:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
    }

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE auth_session, external_identity, app_user CASCADE");
    }

    @AfterAll
    static void stopJwksServer() {
        JWKS_SERVER.stop(0);
    }

    @Test
    void verifiesGoogleSignatureIssuerAudienceExpiryAndEmail() throws Exception {
        assertEquals(200, login(token("bare-issuer", "accounts.google.com", GOOGLE_AUDIENCE,
                Instant.now().plus(5, ChronoUnit.MINUTES), true, true)).statusCode());
        assertEquals(200, login(token("https-issuer", "https://accounts.google.com", GOOGLE_AUDIENCE,
                Instant.now().plus(5, ChronoUnit.MINUTES), true, true)).statusCode());

        assertProblem(login(token("wrong-audience", "https://accounts.google.com", "wrong-audience",
                Instant.now().plus(5, ChronoUnit.MINUTES), true, true)), 401);
        String validToken = token("wrong-signature", "https://accounts.google.com", GOOGLE_AUDIENCE,
                Instant.now().plus(5, ChronoUnit.MINUTES), true, true);
        int signatureStart = validToken.lastIndexOf('.') + 1;
        char signatureFirstCharacter = validToken.charAt(signatureStart);
        String tamperedToken = validToken.substring(0, signatureStart)
                + (signatureFirstCharacter == 'a' ? 'b' : 'a')
                + validToken.substring(signatureStart + 1);
        assertProblem(login(tamperedToken), 401);
        assertProblem(login(token("wrong-issuer", "https://example.com", GOOGLE_AUDIENCE,
                Instant.now().plus(5, ChronoUnit.MINUTES), true, true)), 401);
        assertProblem(login(token("expired", "https://accounts.google.com", GOOGLE_AUDIENCE,
                Instant.now().minus(5, ChronoUnit.MINUTES), true, true)), 401);
        assertProblem(login(token("missing-exp", "https://accounts.google.com", GOOGLE_AUDIENCE,
                null, true, true)), 401);
        assertProblem(login(token("unverified-email", "https://accounts.google.com", GOOGLE_AUDIENCE,
                Instant.now().plus(5, ChronoUnit.MINUTES), false, true)), 401);
        assertProblem(login(token("missing-email", "https://accounts.google.com", GOOGLE_AUDIENCE,
                Instant.now().plus(5, ChronoUnit.MINUTES), true, false)), 401);
    }

    @Test
    void supportsProfilesUniqueUsernamesRotationReplayAndIndependentSessions() throws Exception {
        JsonNode first = successfulLogin("google-user-a", "shared@example.com");
        JsonNode second = successfulLogin("google-user-b", "shared@example.com");
        assertNotEquals(first.at("/user/userID").asText(), second.at("/user/userID").asText());
        assertTrue(first.at("/user/username").isNull());

        String firstAccess = first.get("accessToken").asText();
        String secondAccess = second.get("accessToken").asText();
        assertProblem(request("PUT", "/api/v1/users/me/username", firstAccess,
                "{\"username\":\"Upper_case\"}"), 400);

        HttpResponse<String> usernameSaved = request(
                "PUT",
                "/api/v1/users/me/username",
                firstAccess,
                "{\"username\":\"bravehero123\"}"
        );
        assertEquals(200, usernameSaved.statusCode());
        assertEquals("bravehero123", json(usernameSaved).get("username").asText());
        assertEquals(first.at("/user/userID").asText(), json(usernameSaved).get("userID").asText());
        assertProblem(request("PUT", "/api/v1/users/me/username", secondAccess,
                "{\"username\":\"bravehero123\"}"), 409);

        HttpResponse<String> suggestion = request(
                "POST",
                "/api/v1/users/me/username-suggestions",
                secondAccess,
                null
        );
        assertEquals(200, suggestion.statusCode());
        assertTrue(json(suggestion).get("username").asText().matches("^[a-z0-9]{3,24}$"));
        assertTrue(json(request("GET", "/api/v1/users/me", secondAccess, null)).get("username").isNull());

        JsonNode concurrentA = successfulLogin("concurrent-a", "a@example.com");
        JsonNode concurrentB = successfulLogin("concurrent-b", "b@example.com");
        HttpRequest requestA = requestBuilder("PUT", "/api/v1/users/me/username",
                concurrentA.get("accessToken").asText(), "{\"username\":\"singlename777\"}").build();
        HttpRequest requestB = requestBuilder("PUT", "/api/v1/users/me/username",
                concurrentB.get("accessToken").asText(), "{\"username\":\"singlename777\"}").build();
        CompletableFuture<HttpResponse<String>> futureA = httpClient.sendAsync(
                requestA,
                HttpResponse.BodyHandlers.ofString()
        );
        CompletableFuture<HttpResponse<String>> futureB = httpClient.sendAsync(
                requestB,
                HttpResponse.BodyHandlers.ofString()
        );
        List<Integer> concurrentStatuses = List.of(futureA.join().statusCode(), futureB.join().statusCode());
        assertTrue(concurrentStatuses.contains(200));
        assertTrue(concurrentStatuses.contains(409));

        String originalRefresh = first.get("refreshToken").asText();
        HttpResponse<String> refreshedResponse = request(
                "POST",
                "/api/v1/auth/refresh",
                null,
                objectMapper.writeValueAsString(java.util.Map.of("refreshToken", originalRefresh))
        );
        assertEquals(200, refreshedResponse.statusCode());
        JsonNode refreshed = json(refreshedResponse);
        assertEquals(first.get("sessionExpiresAt").asText(), refreshed.get("sessionExpiresAt").asText());
        assertProblem(request("POST", "/api/v1/auth/refresh", null,
                objectMapper.writeValueAsString(java.util.Map.of("refreshToken", originalRefresh))), 401);
        assertProblem(request("GET", "/api/v1/users/me", refreshed.get("accessToken").asText(), null), 401);
        assertProblem(request("POST", "/api/v1/auth/refresh", null,
                objectMapper.writeValueAsString(java.util.Map.of(
                        "refreshToken", refreshed.get("refreshToken").asText()
                ))), 401);
        assertEquals(200, request("GET", "/api/v1/users/me", secondAccess, null).statusCode());

        JsonNode deviceOne = successfulLogin("same-google-user", "device@example.com");
        JsonNode deviceTwo = successfulLogin("same-google-user", "device@example.com");
        assertEquals(deviceOne.at("/user/userID").asText(), deviceTwo.at("/user/userID").asText());
        assertEquals(204, request("POST", "/api/v1/auth/logout",
                deviceOne.get("accessToken").asText(), null).statusCode());
        assertProblem(request("GET", "/api/v1/users/me", deviceOne.get("accessToken").asText(), null), 401);
        assertProblem(request("POST", "/api/v1/auth/refresh", null,
                objectMapper.writeValueAsString(java.util.Map.of(
                        "refreshToken", deviceOne.get("refreshToken").asText()
                ))), 401);
        assertEquals(200, request("GET", "/api/v1/users/me",
                deviceTwo.get("accessToken").asText(), null).statusCode());
        assertEquals(200, request("POST", "/api/v1/auth/refresh", null,
                objectMapper.writeValueAsString(java.util.Map.of(
                        "refreshToken", deviceTwo.get("refreshToken").asText()
                ))).statusCode());

        JsonNode expiredSession = successfulLogin("expired-session", "expired@example.com");
        String expiredSessionID = SignedJWT.parse(expiredSession.get("refreshToken").asText())
                .getJWTClaimsSet()
                .getStringClaim("session_id");
        jdbcTemplate.update(
                "UPDATE auth_session SET expires_at = now() - interval '1 second' WHERE session_id = ?",
                UUID.fromString(expiredSessionID)
        );
        assertProblem(request("POST", "/api/v1/auth/refresh", null,
                objectMapper.writeValueAsString(java.util.Map.of(
                        "refreshToken", expiredSession.get("refreshToken").asText()
                ))), 401);
        assertProblem(request("GET", "/api/v1/users/me",
                expiredSession.get("accessToken").asText(), null), 401);

        assertProblem(request("GET", "/api/v1/users/me", deviceTwo.get("refreshToken").asText(), null), 401);
        assertProblem(request("POST", "/api/v1/auth/refresh", null,
                objectMapper.writeValueAsString(java.util.Map.of(
                        "refreshToken", deviceTwo.get("accessToken").asText()
                ))), 401);
        assertProblem(request("GET", "/api/v1/users/me", null, null), 401);

        assertProblem(request("POST", "/api/v1/users/me", secondAccess, null), 405);
        HttpRequest unsupportedMediaType = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + serverPort + "/api/v1/users/me/username"))
                .header("Authorization", "Bearer " + secondAccess)
                .header("Content-Type", "text/plain")
                .PUT(HttpRequest.BodyPublishers.ofString("bravehero123"))
                .build();
        assertProblem(httpClient.send(unsupportedMediaType, HttpResponse.BodyHandlers.ofString()), 415);
        HttpRequest unacceptableResponse = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + serverPort + "/api/v1/users/me"))
                .header("Authorization", "Bearer " + secondAccess)
                .header("Accept", "text/plain")
                .GET()
                .build();
        assertProblem(httpClient.send(unacceptableResponse, HttpResponse.BodyHandlers.ofString()), 406);
        assertProblem(request("GET", "/api/v1/unknown", secondAccess, null), 404);
    }

    private JsonNode successfulLogin(String subject, String email) throws Exception {
        HttpResponse<String> response = login(token(
                subject,
                "https://accounts.google.com",
                GOOGLE_AUDIENCE,
                Instant.now().plus(5, ChronoUnit.MINUTES),
                true,
                true,
                email
        ));
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = json(response);
        assertNotNull(body.get("accessToken"));
        assertNotNull(body.get("refreshToken"));
        return body;
    }

    private HttpResponse<String> login(String identityToken) throws Exception {
        return request(
                "POST",
                "/api/v1/auth/google",
                null,
                objectMapper.writeValueAsString(java.util.Map.of("identityToken", identityToken))
        );
    }

    private HttpResponse<String> request(String method, String path, String accessToken, String body)
            throws IOException, InterruptedException {
        return httpClient.send(
                requestBuilder(method, path, accessToken, body).build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpRequest.Builder requestBuilder(String method, String path, String accessToken, String body) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + serverPort + path))
                .method(method, publisher)
                .header("Content-Type", "application/json");
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return builder;
    }

    private JsonNode json(HttpResponse<String> response) throws IOException {
        return objectMapper.readTree(response.body());
    }

    private void assertProblem(HttpResponse<String> response, int expectedStatus) throws IOException {
        assertEquals(expectedStatus, response.statusCode(), response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));
        JsonNode problem = json(response);
        assertEquals(expectedStatus, problem.get("status").asInt());
        assertFalse(problem.get("title").asText().isBlank());
        assertFalse(problem.get("detail").asText().isBlank());
    }

    private static String token(
            String subject,
            String issuer,
            String audience,
            Instant expiresAt,
            boolean emailVerified,
            boolean includeEmail
    ) throws Exception {
        return token(subject, issuer, audience, expiresAt, emailVerified, includeEmail, "hero@example.com");
    }

    private static String token(
            String subject,
            String issuer,
            String audience,
            Instant expiresAt,
            boolean emailVerified,
            boolean includeEmail,
            String email
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .claim("email_verified", emailVerified)
                .claim("picture", "https://example.com/avatar.png");
        if (expiresAt != null) {
            claims.expirationTime(Date.from(expiresAt));
        }
        if (includeEmail) {
            claims.claim("email", email);
        }
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(GOOGLE_SIGNING_KEY.getKeyID()).build(),
                claims.build()
        );
        jwt.sign(new RSASSASigner(GOOGLE_SIGNING_KEY));
        return jwt.serialize();
    }

    private static RSAKey createSigningKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID("phase2-google-test-key")
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Google test key", exception);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            String jwks = new JWKSet(GOOGLE_SIGNING_KEY.toPublicJWK()).toString();
            server.createContext("/jwks", exchange -> {
                byte[] response = jwks.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start test JWKS server", exception);
        }
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
