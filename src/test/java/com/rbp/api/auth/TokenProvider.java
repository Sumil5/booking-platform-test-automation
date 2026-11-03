package com.rbp.api.auth;

import com.rbp.api.client.Specs;
import com.rbp.api.config.Config;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;

/**
 * Supplies the admin token for the whole test run.
 *
 * <p>It logs in once, on first use, and caches the token. The platform resets its
 * databases every few minutes, and the auth reset deletes every token. Two safeguards
 * deal with that:
 * <ul>
 *   <li>Proactive: a cached token is re-validated (one cheap {@code POST /api/auth/validate})
 *       when it was last confirmed more than {@code TOKEN_REVALIDATE_SECONDS} ago.</li>
 *   <li>Reactive: {@link #replaceIfRevoked(String)} lets a client recover when a reset
 *       lands inside that window, without ever rescuing a deliberately bad token.</li>
 * </ul>
 * Thread-safe, so it also works with parallel scenarios.
 */
public final class TokenProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TokenProvider.class);
    /** A token confirmed this recently is trusted without another validate call. */
    private static final Duration RECHECK_GRACE = Duration.ofSeconds(2);
    private static volatile TokenProvider instance;

    private final Config config;
    private final AtomicInteger loginCount = new AtomicInteger();
    /** Every token this provider has issued, so deliberately invalid test tokens are never "recovered". */
    private final Set<String> issued = new HashSet<>();
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

    /**
     * Called by a client after a request that carried {@code used} came back with a status a
     * revoked token can cause. Returns the token to replay the request with, or empty when
     * the response must stand as it is:
     * <ul>
     *   <li>empty if this provider never issued {@code used}: an invalid or tampered token
     *       sent on purpose by a negative test is never swapped for a good one;</li>
     *   <li>the current token if {@code used} is an older one that was already replaced;</li>
     *   <li>empty if {@code used} is still valid, so the response is genuine;</li>
     *   <li>a new token if the server confirms {@code used} was revoked.</li>
     * </ul>
     */
    public synchronized Optional<String> replaceIfRevoked(String used) {
        if (used == null || !issued.contains(used)) {
            return Optional.empty();
        }
        if (!used.equals(token)) {
            return Optional.of(token);
        }
        if (Duration.between(lastConfirmed, Instant.now()).compareTo(RECHECK_GRACE) < 0 || isStillValid(token)) {
            return Optional.empty();
        }
        LOG.warn("Admin token was revoked mid-run (platform reset?), logging in again and replaying the request");
        token = login();
        return Optional.of(token);
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
        String newToken = response.jsonPath().getString("token");
        if (newToken == null || newToken.isBlank()) {
            throw new IllegalStateException("Admin login returned no token. Body: " + response.asString());
        }
        loginCount.incrementAndGet();
        issued.add(newToken);
        lastConfirmed = Instant.now();
        LOG.info("Logged in as admin (login #{} this run)", loginCount.get());
        return newToken;
    }
}
