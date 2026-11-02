package com.rbp.api.client;

import com.rbp.api.support.HttpExchangeLog;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.Optional;

/** {@code /api/room}: public reads, admin writes. */
public class RoomClient extends BaseClient {

    public RoomClient(HttpExchangeLog log) {
        super(log);
    }

    public Response list() {
        return request().get("/api/room");
    }

    public Response listAvailable(LocalDate checkin, LocalDate checkout) {
        return request()
                .queryParam("checkin", checkin.toString())
                .queryParam("checkout", checkout.toString())
                .get("/api/room");
    }

    public Response get(int roomId) {
        return request().get("/api/room/{id}", roomId);
    }

    public Response create(Object body, Optional<String> token) {
        return request(token).body(body).post("/api/room");
    }

    public Response update(int roomId, Object body, Optional<String> token) {
        return request(token).body(body).put("/api/room/{id}", roomId);
    }

    public Response delete(int roomId, Optional<String> token) {
        return request(token).delete("/api/room/{id}", roomId);
    }
}
