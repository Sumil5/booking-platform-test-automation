package com.rbp.api.data;

import com.rbp.api.model.BookingDates;
import com.rbp.api.model.BookingPayload;
import com.rbp.api.model.MessagePayload;
import com.rbp.api.model.RoomPayload;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds valid, unique test data. The target environment is shared and reset every
 * few minutes, so every scenario creates its own records. Each record gets a unique
 * marker so it can be found again, and so nobody mistakes it for real data.
 */
public final class TestDataFactory {

    /** Prefix on every generated name, so leftovers are easy to spot on the shared site. */
    public static final String MARKER = "AT";

    private TestDataFactory() {
    }

    public static String uniqueId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public static RoomPayload room() {
        return room("Double", 150);
    }

    public static RoomPayload room(String type, int price) {
        return new RoomPayload(
                MARKER + " Room " + uniqueId(),
                type,
                true,
                "/images/room2.jpg",
                "Room created by the API test suite",
                List.of("WiFi", "TV", "Safe"),
                price);
    }

    public static BookingPayload booking(int roomId, int nights) {
        return new BookingPayload(
                roomId,
                "Guest" + letters(6),
                "Tester" + letters(6),
                true,
                futureDates(nights),
                null,
                null);
    }

    /**
     * Dates well in the future, randomised so repeated runs against the same room
     * are unlikely to overlap.
     */
    public static BookingDates futureDates(int nights) {
        LocalDate checkin = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(400, 3_000));
        return new BookingDates(checkin, checkin.plusDays(nights));
    }

    public static MessagePayload message() {
        String id = uniqueId();
        return new MessagePayload(
                MARKER + " Sender " + id,
                "at-" + id + "@example.com",
                "01234567890",
                MARKER + " enquiry " + id,
                "Automated test message, safe to delete. Reference " + id + ".");
    }

    private static String letters(int count) {
        StringBuilder sb = new StringBuilder(count);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }
}
