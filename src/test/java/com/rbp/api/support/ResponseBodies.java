package com.rbp.api.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.rbp.api.client.Specs;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads response bodies that come in more than one shape. The public {@code /api}
 * paths are served either by a Next.js handler or by the Spring service directly (see
 * docs/API.md), and each one wraps bodies differently.
 */
public final class ResponseBodies {

    private ResponseBodies() {
    }

    public static JsonNode json(Response response) {
        String body = response.asString();
        if (body == null || body.isBlank()) {
            return Specs.MAPPER.missingNode();
        }
        try {
            return Specs.MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Response body is not JSON (HTTP " + response.statusCode() + "): " + body, e);
        }
    }

    /**
     * All error messages in an error body. Handles {@code {"errors":[...]}},
     * {@code {"fieldErrors":[...]}}, {@code {"error":"..."}} and a bare JSON array.
     */
    public static List<String> errorMessages(Response response) {
        JsonNode root = json(response);
        List<String> messages = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(n -> messages.add(n.asText()));
            return messages;
        }
        for (String field : List.of("errors", "fieldErrors", "error", "errorMessage")) {
            JsonNode node = root.path(field);
            if (node.isArray()) {
                node.forEach(n -> messages.add(n.asText()));
            } else if (node.isTextual()) {
                messages.add(node.asText());
            }
        }
        return messages;
    }

    /**
     * The booking object in a booking response. Next.js returns the booking flat;
     * the Spring service wraps it as {@code {"bookingid":..,"booking":{..}}}.
     */
    public static JsonNode booking(Response response) {
        JsonNode root = json(response);
        return root.has("booking") && root.get("booking").isObject() ? root.get("booking") : root;
    }

    public static int bookingId(Response response) {
        JsonNode root = json(response);
        JsonNode id = root.path("bookingid");
        if (!id.isInt() || id.asInt() <= 0) {
            id = booking(response).path("bookingid");
        }
        if (!id.isInt() || id.asInt() <= 0) {
            throw new AssertionError("No booking id in response: " + response.asString());
        }
        return id.asInt();
    }
}
