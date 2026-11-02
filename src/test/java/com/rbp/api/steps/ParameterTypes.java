package com.rbp.api.steps;

import com.rbp.api.auth.TokenVariant;
import io.cucumber.java.ParameterType;

public class ParameterTypes {

    /** {@code {token}}: "a valid", "no", "an invalid" or "a tampered". */
    @ParameterType("a valid|no|an invalid|a tampered")
    public TokenVariant token(String phrase) {
        return TokenVariant.fromPhrase(phrase);
    }

    /** {@code {adminAction}}: a protected operation on the record the scenario created. */
    @ParameterType("update the room|delete the room|fetch the booking|update the booking|delete the booking"
            + "|list the bookings for the room|mark the message as read|delete the message")
    public AdminAction adminAction(String phrase) {
        return AdminAction.fromPhrase(phrase);
    }
}
