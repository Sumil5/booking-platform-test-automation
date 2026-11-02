package com.rbp.api.client;

import com.rbp.api.support.HttpExchangeLog;
import io.restassured.response.Response;

import java.util.Optional;

/** {@code /api/booking}: public create, admin reads and writes. */
public class BookingClient extends BaseClient {

    public BookingClient(HttpExchangeLog log) {
        super(log);
    }

    public Response create(Object body) {
        return request().body(body).post("/api/booking");
    }

    public Response get(int bookingId, Optional<String> token) {
        return request(token).get("/api/booking/{id}", bookingId);
    }

    public Response listForRoom(int roomId, Optional<String> token) {
        return request(token).queryParam("roomid", roomId).get("/api/booking");
    }

    public Response update(int bookingId, Object body, Optional<String> token) {
        return request(token).body(body).put("/api/booking/{id}", bookingId);
    }

    public Response delete(int bookingId, Optional<String> token) {
        return request(token).delete("/api/booking/{id}", bookingId);
    }
}
