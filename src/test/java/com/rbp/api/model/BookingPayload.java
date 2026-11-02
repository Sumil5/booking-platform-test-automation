package com.rbp.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for creating or updating a booking.
 * {@code email} and {@code phone} are optional and left out when null. When both are
 * sent, the platform also creates a contact message, so the default test data omits them.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingPayload(
        int roomid,
        String firstname,
        String lastname,
        boolean depositpaid,
        BookingDates bookingdates,
        String email,
        String phone) {

    public BookingPayload withNames(String newFirstname, String newLastname) {
        return new BookingPayload(roomid, newFirstname, newLastname, depositpaid, bookingdates, email, phone);
    }

    public BookingPayload withDates(BookingDates newDates) {
        return new BookingPayload(roomid, firstname, lastname, depositpaid, newDates, email, phone);
    }
}
