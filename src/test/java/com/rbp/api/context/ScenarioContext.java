package com.rbp.api.context;

import com.rbp.api.model.BookingPayload;
import com.rbp.api.model.MessagePayload;
import com.rbp.api.model.RoomPayload;
import io.restassured.response.Response;

/**
 * State shared between the step classes of a single scenario. PicoContainer creates
 * a new instance for every scenario, so nothing leaks between scenarios.
 */
public class ScenarioContext {

    private final CleanupRegistry cleanup = new CleanupRegistry();

    private Response lastResponse;

    private Integer roomId;
    private RoomPayload room;

    private Integer bookingId;
    private BookingPayload booking;

    private Integer messageId;
    private MessagePayload message;

    public CleanupRegistry cleanup() {
        return cleanup;
    }

    public Response lastResponse() {
        if (lastResponse == null) {
            throw new IllegalStateException("No request has been sent in this scenario yet");
        }
        return lastResponse;
    }

    public void lastResponse(Response response) {
        this.lastResponse = response;
    }

    public int roomId() {
        return require(roomId, "room");
    }

    public RoomPayload room() {
        return require(room, "room");
    }

    public void room(int id, RoomPayload payload) {
        this.roomId = id;
        this.room = payload;
    }

    public void room(RoomPayload payload) {
        this.room = payload;
    }

    public int bookingId() {
        return require(bookingId, "booking");
    }

    public BookingPayload booking() {
        return require(booking, "booking");
    }

    public void booking(int id, BookingPayload payload) {
        this.bookingId = id;
        this.booking = payload;
    }

    public void booking(BookingPayload payload) {
        this.booking = payload;
    }

    public int messageId() {
        return require(messageId, "message");
    }

    public MessagePayload message() {
        return require(message, "message");
    }

    public void message(int id, MessagePayload payload) {
        this.messageId = id;
        this.message = payload;
    }

    public void message(MessagePayload payload) {
        this.message = payload;
    }

    private static <T> T require(T value, String what) {
        if (value == null) {
            throw new IllegalStateException("No " + what + " in this scenario; add a Given step that creates one");
        }
        return value;
    }
}
