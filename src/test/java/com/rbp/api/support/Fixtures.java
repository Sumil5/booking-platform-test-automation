package com.rbp.api.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbp.api.auth.TokenProvider;
import com.rbp.api.client.BookingClient;
import com.rbp.api.client.MessageClient;
import com.rbp.api.client.RoomClient;
import com.rbp.api.context.ScenarioContext;
import com.rbp.api.model.BookingPayload;
import com.rbp.api.model.MessagePayload;
import com.rbp.api.model.RoomPayload;
import io.restassured.response.Response;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Creates the test data a scenario needs, records it in the {@link ScenarioContext},
 * and registers its cleanup.
 */
public class Fixtures {

    /** An id far above anything the platform reaches between resets. */
    public static final int NON_EXISTENT_ID = 9_999_999;

    private final ScenarioContext context;
    private final RoomClient rooms;
    private final BookingClient bookings;
    private final MessageClient messages;

    public Fixtures(ScenarioContext context, RoomClient rooms, BookingClient bookings, MessageClient messages) {
        this.context = context;
        this.rooms = rooms;
        this.bookings = bookings;
        this.messages = messages;
    }

    public static Optional<String> adminToken() {
        return Optional.of(TokenProvider.get().token());
    }

    /**
     * Creates a room as admin. The API does not return the new id, so this method
     * finds the room again by its unique name.
     */
    public Response createRoom(RoomPayload payload) {
        context.room(payload);
        Response response = rooms.create(payload, adminToken());
        if (isSuccess(response)) {
            int id = findRoomId(payload.roomName());
            context.room(id, payload);
            context.cleanup().register("delete room " + id, () -> rooms.delete(id, adminToken()));
        }
        return response;
    }

    public int findRoomId(String roomName) {
        JsonNode room = findInList(rooms.list(), "rooms", n -> roomName.equals(n.path("roomName").asText()))
                .orElseThrow(() -> new AssertionError("Room '" + roomName + "' not found in GET /api/room"));
        return room.path("roomid").asInt();
    }

    public Response createBooking(BookingPayload payload) {
        context.booking(payload);
        Response response = bookings.create(payload);
        if (response.statusCode() == 201) {
            int id = ResponseBodies.bookingId(response);
            context.booking(id, payload);
            context.cleanup().register("delete booking " + id, () -> bookings.delete(id, adminToken()));
        }
        return response;
    }

    /** Sends a contact message. The API does not return its id, so this finds it by its unique subject. */
    public Response sendMessage(MessagePayload payload) {
        context.message(payload);
        Response response = messages.create(payload);
        if (isSuccess(response)) {
            JsonNode summary = findMessageSummary(payload.subject())
                    .orElseThrow(() -> new AssertionError("Message '" + payload.subject() + "' not found in GET /api/message"));
            int id = summary.path("id").asInt();
            context.message(id, payload);
            context.cleanup().register("delete message " + id, () -> messages.delete(id, adminToken()));
        }
        return response;
    }

    public Optional<JsonNode> findMessageSummary(String subject) {
        return findInList(messages.list(), "messages", n -> subject.equals(n.path("subject").asText()));
    }

    public static Optional<JsonNode> findInList(Response response, String arrayField, Predicate<JsonNode> match) {
        for (JsonNode node : ResponseBodies.json(response).path(arrayField)) {
            if (match.test(node)) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    public static boolean isSuccess(Response response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /** Fails setup with a clear message, so the scenario is not blamed for a broken precondition. */
    public static Response requireSuccess(Response response, String what) {
        if (!isSuccess(response)) {
            throw new IllegalStateException("Test data setup failed: " + what + " returned HTTP "
                    + response.statusCode() + " " + response.asString());
        }
        return response;
    }
}
