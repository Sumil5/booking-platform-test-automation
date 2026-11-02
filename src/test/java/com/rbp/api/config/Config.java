package com.rbp.api.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Environment-agnostic configuration.
 *
 * <p>Each key is resolved in this order: JVM system property ({@code -DKEY=value}),
 * environment variable, then an optional, git-ignored {@code .env} file in the
 * working directory. No value has a default in code; required keys that are missing
 * fail the run immediately with a message listing every missing key.
 */
public final class Config {

    public static final String BASE_URL = "BASE_URL";
    public static final String ADMIN_USERNAME = "ADMIN_USERNAME";
    public static final String ADMIN_PASSWORD = "ADMIN_PASSWORD";
    public static final String HTTP_TIMEOUT_MS = "HTTP_TIMEOUT_MS";
    public static final String TOKEN_REVALIDATE_SECONDS = "TOKEN_REVALIDATE_SECONDS";

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
    private static volatile Config instance;

    private final String baseUrl;
    private final String adminUsername;
    private final String adminPassword;
    private final int httpTimeoutMs;
    private final long tokenRevalidateSeconds;

    private Config() {
        List<String> missing = new ArrayList<>();
        this.baseUrl = stripTrailingSlash(required(BASE_URL, missing));
        this.adminUsername = required(ADMIN_USERNAME, missing);
        this.adminPassword = required(ADMIN_PASSWORD, missing);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required configuration: " + missing
                    + ". Set them as environment variables, -D system properties, or in a .env file"
                    + " (see .env.example).");
        }
        this.httpTimeoutMs = Integer.parseInt(optional(HTTP_TIMEOUT_MS).orElse("15000"));
        this.tokenRevalidateSeconds = Long.parseLong(optional(TOKEN_REVALIDATE_SECONDS).orElse("60"));
    }

    public static Config get() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    public static Optional<String> optional(String key) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            value = System.getenv(key);
        }
        if (isBlank(value)) {
            value = DOTENV.get(key);
        }
        return isBlank(value) ? Optional.empty() : Optional.of(value.trim());
    }

    private static String required(String key, List<String> missing) {
        return optional(key).orElseGet(() -> {
            missing.add(key);
            return null;
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripTrailingSlash(String url) {
        return url == null ? null : url.replaceAll("/+$", "");
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String adminUsername() {
        return adminUsername;
    }

    public String adminPassword() {
        return adminPassword;
    }

    public int httpTimeoutMs() {
        return httpTimeoutMs;
    }

    public long tokenRevalidateSeconds() {
        return tokenRevalidateSeconds;
    }
}
