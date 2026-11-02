package com.rbp.api.support;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Records every HTTP exchange made during one scenario so it can be attached to the
 * Cucumber report. Token cookies and passwords are masked before they are stored.
 *
 * <p>One instance per scenario (created by PicoContainer).
 */
public class HttpExchangeLog implements Filter {

    private static final Pattern TOKEN_COOKIE = Pattern.compile("(token=)([^;\\s]{4})[^;\\s]*");
    private static final Pattern JSON_TOKEN = Pattern.compile("(\"token\"\\s*:\\s*\")([^\"]{0,4})[^\"]*(\")");
    private static final Pattern JSON_PASSWORD = Pattern.compile("(\"password\"\\s*:\\s*\")[^\"]*(\")");

    private final List<String> exchanges = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        long start = System.nanoTime();
        Response response = ctx.next(request, responseSpec);
        long millis = (System.nanoTime() - start) / 1_000_000;

        StringBuilder sb = new StringBuilder()
                .append(request.getMethod()).append(' ').append(request.getURI()).append('\n');
        request.getCookies().forEach(c -> sb.append("Cookie: ").append(mask(c.getName() + "=" + c.getValue())).append('\n'));
        Object body = request.getBody();
        if (body != null) {
            sb.append(mask(String.valueOf(body))).append('\n');
        }
        sb.append("\n--> ").append(response.getStatusLine()).append(" (").append(millis).append(" ms)\n");
        String responseBody = response.getBody().asString();
        if (responseBody != null && !responseBody.isEmpty()) {
            sb.append(mask(responseBody)).append('\n');
        }
        exchanges.add(sb.toString());
        return response;
    }

    /** Keeps the first 4 characters of a token (to tell tokens apart) and hides passwords completely. */
    public static String mask(String text) {
        String masked = TOKEN_COOKIE.matcher(text).replaceAll("$1$2****");
        masked = JSON_TOKEN.matcher(masked).replaceAll("$1$2****$3");
        return JSON_PASSWORD.matcher(masked).replaceAll("$1****$2");
    }

    public String render() {
        synchronized (exchanges) {
            return String.join("\n==============================\n", exchanges);
        }
    }

    public boolean isEmpty() {
        return exchanges.isEmpty();
    }
}
