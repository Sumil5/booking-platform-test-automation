package com.rbp.api.model;

import java.util.List;

/** Request body for creating or updating a room. */
public record RoomPayload(
        String roomName,
        String type,
        boolean accessible,
        String image,
        String description,
        List<String> features,
        int roomPrice) {

    public RoomPayload withType(String newType) {
        return new RoomPayload(roomName, newType, accessible, image, description, features, roomPrice);
    }

    public RoomPayload withPrice(int newPrice) {
        return new RoomPayload(roomName, type, accessible, image, description, features, newPrice);
    }
}
