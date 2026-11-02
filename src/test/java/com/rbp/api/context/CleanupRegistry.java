package com.rbp.api.context;

import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * Teardown actions for the data a scenario created. They run in reverse order of
 * registration (bookings before the room they belong to). A 404 counts as success,
 * because the scenario may already have deleted the record or the platform may have
 * reset. Failures are reported but never fail the scenario.
 */
public class CleanupRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupRegistry.class);

    private record Action(String description, Supplier<Response> call) {
    }

    private final Deque<Action> actions = new ArrayDeque<>();

    public void register(String description, Supplier<Response> call) {
        actions.push(new Action(description, call));
    }

    /** Runs every action and returns a line per action for the report. */
    public List<String> runAll() {
        List<String> outcomes = new ArrayList<>();
        while (!actions.isEmpty()) {
            Action action = actions.pop();
            try {
                int status = action.call().get().statusCode();
                boolean ok = (status >= 200 && status < 300) || status == 404;
                String line = (ok ? "OK   " : "WARN ") + action.description() + " -> HTTP " + status;
                if (!ok) {
                    LOG.warn("Cleanup did not succeed: {}", line);
                }
                outcomes.add(line);
            } catch (RuntimeException e) {
                LOG.warn("Cleanup failed: {}", action.description(), e);
                outcomes.add("FAIL " + action.description() + " -> " + e);
            }
        }
        return outcomes;
    }
}
