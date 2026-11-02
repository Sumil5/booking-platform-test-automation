package com.rbp.api.auth;

import com.rbp.api.client.Specs;
import com.rbp.api.config.Config;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;

/**
 * Supplies the admin token for the whole test run.
 *
 * <p>It logs in once, on first use, and caches the token. The platform resets its
 * databases every few minutes, and the auth reset deletes every token. So a cached
 * token is re-validated (one cheap {@code POST /api/auth/validate}) when it was last
 * confirmed more than {@code TOKEN_REVALIDATE_SECONDS} ago. The provider logs in again
 * only if that check fails. Thread-safe, so it also works with parallel scenarios.
 */
public final class TokenProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TokenProvider.class);
    private static volatile TokenProvider instance;

    private final Config config;
    private final AtomicInteger loginCount = new AtomicInteger();
    private String token;
    private Instant lastConfirmed = Instant.EPOCH;

    private TokenProvider(Config config) {
        this.config = config;
    }

    /** Created on first use, so a configuration error surfaces as a clear message rather than a class-init failure. */
    public static TokenProvider get() {
        if (instance == null) {
            synchronized (TokenProvider.class) {
                if (instance == null) {
                    instance = new TokenProvider(Config.get());
                }
            }
        }
        return instance;
    }

    public synchronized String token() {
        if (token == null) {
            token = login();
        } else if (isStale() && !isStillValid(token)) {
            LOG.info("Cached admin token is no longer valid (platform reset?), logging in again");
            token = login();
        }
        return token;
    }

    /** Number of logins performed by this provider during the run. */
    public int loginCount() {
        return loginCount.get();
    }

    private boolean isStale() {
        return Duration.between(lastConfirmed, Instant.now()).toSeconds() >= config.tokenRevalidateSeconds();
    }

    private boolean isStillValid(String candidate) {
        Response response = given(Specs.base())
                .body(Map.of("token", candidate))
                .post("/api/auth/validate");
        boolean valid = response.statusCode() == 200;
        if (valid) {
            lastConfirmed = Instant.now();
        }
        return valid;
    }

    private String login() {
        Response response = given(Specs.base())
                .body(Map.of("username", config.adminUsername(), "password", config.adminPassword()))
                .post("/api/auth/login");
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Admin login to " + config.baseUrl() + " failed with HTTP "
                    + response.statusCode() + ": " + response.asString()
                    + " (check BASE_URL, ADMIN_USERNAME and ADMIN_PASSWORD)");
        }
        String issued = response.jsonPath().getString("token");
        if (issued == null || issued.isBlank()) {
            throw new IllegalStateException("Admin login returned no token. Body: " + response.asString());
        }
        loginCount.incrementAndGet();
        lastConfirmed = Instant.now();
        LOG.info("Logged in as admin (login #{} this run)", loginCount.get());
        return issued;
    }
}
