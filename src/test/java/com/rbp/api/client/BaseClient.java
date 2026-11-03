package com.rbp.api.client;

import com.rbp.api.auth.TokenProvider;
import com.rbp.api.support.HttpExchangeLog;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static io.restassured.RestAssured.given;

/** Common plumbing for the service clients: base spec, report logging and the token cookie. */
abstract class BaseClient {

    /**
     * Statuses a revoked token can produce on this platform: 401 or 403 from the services,
     * and 500 where a Next.js handler fails to parse the services' empty 403 body.
     */
    private static final Set<Integer> REVOKED_TOKEN_STATUSES = Set.of(401, 403, 500);

    private final HttpExchangeLog log;

    BaseClient(HttpExchangeLog log) {
        this.log = log;
    }

    /** A request with no token cookie. */
    RequestSpecification request() {
        return given(Specs.base()).filter(log);
    }

    /** A request carrying {@code token} as the auth cookie, or no cookie when empty. */
    RequestSpecification request(Optional<String> token) {
        RequestSpecification spec = request();
        token.ifPresent(t -> spec.cookie("token", t));
        return spec;
    }

    /**
     * Sends an authenticated request. If the platform reset and revoked the run's admin token
     * mid-scenario, the request is replayed once with a fresh token. {@link TokenProvider}
     * decides: it only replaces a token it issued itself and the server has confirmed dead, so
     * negative tests that send no, invalid or tampered tokens get the server's answer untouched.
     * Both attempts go through the logging filter, so the report shows the original failure.
     */
    Response send(Optional<String> token, Function<RequestSpecification, Response> call) {
        Response response = call.apply(request(token));
        if (token.isPresent() && REVOKED_TOKEN_STATUSES.contains(response.statusCode())) {
            Optional<String> fresh = TokenProvider.get().replaceIfRevoked(token.get());
            if (fresh.isPresent()) {
                response = call.apply(request(fresh));
            }
        }
        return response;
    }
}
