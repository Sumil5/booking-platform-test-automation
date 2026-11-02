package com.rbp.api.auth;

import java.util.Optional;
import java.util.UUID;

/** The kinds of token a scenario can send, from a real one to deliberately broken ones. */
public enum TokenVariant {

    /** The cached admin token from {@link TokenProvider}. */
    VALID,
    /** No token cookie at all. */
    NONE,
    /** A random value the platform never issued. */
    INVALID,
    /** A real token with its last character changed. */
    TAMPERED;

    public static TokenVariant fromPhrase(String phrase) {
        return switch (phrase) {
            case "a valid" -> VALID;
            case "no" -> NONE;
            case "an invalid" -> INVALID;
            case "a tampered" -> TAMPERED;
            default -> throw new IllegalArgumentException("Unknown token phrase: " + phrase);
        };
    }

    /** The cookie value to send, or empty for {@link #NONE}. */
    public Optional<String> resolve() {
        return switch (this) {
            case VALID -> Optional.of(TokenProvider.get().token());
            case NONE -> Optional.empty();
            case INVALID -> Optional.of(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            case TAMPERED -> Optional.of(tamper(TokenProvider.get().token()));
        };
    }

    static String tamper(String token) {
        char last = token.charAt(token.length() - 1);
        char replacement = last == 'a' ? 'b' : 'a';
        return token.substring(0, token.length() - 1) + replacement;
    }
}
