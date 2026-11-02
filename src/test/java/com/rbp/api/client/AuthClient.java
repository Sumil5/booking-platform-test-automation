package com.rbp.api.client;

import com.rbp.api.support.HttpExchangeLog;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** {@code /api/auth}: login and token validation. */
public class AuthClient extends BaseClient {

    public AuthClient(HttpExchangeLog log) {
        super(log);
    }

    public Response login(String username, String password) {
        return request().body(Map.of("username", username, "password", password)).post("/api/auth/login");
    }

    public Response validate(Optional<String> token) {
        Map<String, String> body = new HashMap<>();
        token.ifPresent(t -> body.put("token", t));
        return request().body(body).post("/api/auth/validate");
    }
}
