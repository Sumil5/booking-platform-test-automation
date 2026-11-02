package com.rbp.api.steps;

import com.rbp.api.auth.TokenVariant;
import com.rbp.api.client.BookingClient;
import com.rbp.api.client.MessageClient;
import com.rbp.api.client.RoomClient;
import com.rbp.api.context.ScenarioContext;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.Optional;

/** One step that runs any protected operation with a chosen kind of token. */
public class AuthorisationSteps {

    private final ScenarioContext context;
    private final RoomClient rooms;
    private final BookingClient bookings;
    private final MessageClient messages;

    public AuthorisationSteps(ScenarioContext context, RoomClient rooms, BookingClient bookings, MessageClient messages) {
        this.context = context;
        this.rooms = rooms;
        this.bookings = bookings;
        this.messages = messages;
    }

    @When("I try to {adminAction} with {token} token")
    public void iTryToWithToken(AdminAction action, TokenVariant token) {
        Optional<String> cookie = token.resolve();
        Response response = switch (action) {
            case UPDATE_ROOM -> rooms.update(context.roomId(), context.room().withPrice(199), cookie);
            case DELETE_ROOM -> rooms.delete(context.roomId(), cookie);
            case FETCH_BOOKING -> bookings.get(context.bookingId(), cookie);
            case UPDATE_BOOKING -> bookings.update(context.bookingId(),
                    context.booking().withNames("Mallory", "Intruder"), cookie);
            case DELETE_BOOKING -> bookings.delete(context.bookingId(), cookie);
            case LIST_BOOKINGS -> bookings.listForRoom(context.roomId(), cookie);
            case MARK_MESSAGE_READ -> messages.markRead(context.messageId(), cookie);
            case DELETE_MESSAGE -> messages.delete(context.messageId(), cookie);
        };
        context.lastResponse(response);
    }
}
