package com.rbp.api.client;

import com.rbp.api.support.HttpExchangeLog;
import io.restassured.specification.RequestSpecification;

import java.util.Optional;

import static io.restassured.RestAssured.given;

/** Common plumbing for the service clients: base spec, report logging and the token cookie. */
abstract class BaseClient {

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
}
