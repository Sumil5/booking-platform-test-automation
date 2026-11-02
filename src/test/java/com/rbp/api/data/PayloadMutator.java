package com.rbp.api.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rbp.api.client.Specs;

/**
 * Turns a valid payload into an invalid one by changing a single field, so that
 * validation scenarios can be written as Scenario Outline tables.
 *
 * <p>{@code path} may be dotted ({@code bookingdates.checkin}). {@code jsonValue} is a JSON
 * literal ({@code ""}, {@code "ab"}, {@code 0}, {@code null}), or {@code <omit>} to remove
 * the field.
 */
public final class PayloadMutator {

    public static final String OMIT = "<omit>";

    private PayloadMutator() {
    }

    public static ObjectNode with(Object validPayload, String path, String jsonValue) {
        ObjectNode root = Specs.MAPPER.valueToTree(validPayload);
        String[] parts = path.split("\\.");
        ObjectNode parent = root;
        for (int i = 0; i < parts.length - 1; i++) {
            parent = (ObjectNode) parent.get(parts[i]);
        }
        String field = parts[parts.length - 1];
        if (OMIT.equals(jsonValue.trim())) {
            parent.remove(field);
        } else {
            parent.set(field, parse(jsonValue));
        }
        return root;
    }

    private static JsonNode parse(String jsonValue) {
        try {
            return Specs.MAPPER.readTree(jsonValue);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not a JSON literal: " + jsonValue
                    + " (quote strings, e.g. \"abc\")", e);
        }
    }
}
