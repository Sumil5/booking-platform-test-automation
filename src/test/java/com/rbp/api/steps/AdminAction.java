package com.rbp.api.steps;

import java.util.Arrays;

/** Operations that need an admin token, as phrased in the feature files. */
public enum AdminAction {
    UPDATE_ROOM("update the room"),
    DELETE_ROOM("delete the room"),
    FETCH_BOOKING("fetch the booking"),
    UPDATE_BOOKING("update the booking"),
    DELETE_BOOKING("delete the booking"),
    LIST_BOOKINGS("list the bookings for the room"),
    MARK_MESSAGE_READ("mark the message as read"),
    DELETE_MESSAGE("delete the message");

    private final String phrase;

    AdminAction(String phrase) {
        this.phrase = phrase;
    }

    public static AdminAction fromPhrase(String phrase) {
        return Arrays.stream(values())
                .filter(a -> a.phrase.equals(phrase))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown action: " + phrase));
    }
}
