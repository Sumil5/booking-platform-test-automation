package com.rbp.api.client;

import com.rbp.api.support.HttpExchangeLog;
import io.restassured.response.Response;

import java.util.Optional;

/** {@code /api/message}: public contact form, admin inbox operations. */
public class MessageClient extends BaseClient {

    public MessageClient(HttpExchangeLog log) {
        super(log);
    }

    public Response create(Object body) {
        return request().body(body).post("/api/message");
    }

    public Response list() {
        return request().get("/api/message");
    }

    public Response get(int messageId) {
        return request().get("/api/message/{id}", messageId);
    }

    public Response count() {
        return request().get("/api/message/count");
    }

    public Response markRead(int messageId, Optional<String> token) {
        return request(token).put("/api/message/{id}/read", messageId);
    }

    public Response delete(int messageId, Optional<String> token) {
        return request(token).delete("/api/message/{id}", messageId);
    }
}
