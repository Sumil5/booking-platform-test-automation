package com.rbp.api.model;

/** Request body for the public contact form. */
public record MessagePayload(String name, String email, String phone, String subject, String description) {
}
