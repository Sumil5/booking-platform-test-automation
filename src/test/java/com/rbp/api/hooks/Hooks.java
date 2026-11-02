package com.rbp.api.hooks;

import com.rbp.api.config.Config;
import com.rbp.api.context.ScenarioContext;
import com.rbp.api.support.HttpExchangeLog;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.opentest4j.TestAbortedException;

import java.util.List;

public class Hooks {

    private final ScenarioContext context;
    private final HttpExchangeLog httpLog;

    public Hooks(ScenarioContext context, HttpExchangeLog httpLog) {
        this.context = context;
        this.httpLog = httpLog;
    }

    /**
     * Skips scenarios tagged {@code @known-defect} unless {@code RUN_KNOWN_DEFECTS=true}.
     * Skipping, rather than filtering them out by tag, keeps them visible in every report
     * with the reason attached, and works with whatever tag expression the run uses.
     * Order 0 runs this before the Background, so no test data is created for them.
     */
    @Before(value = "@known-defect", order = 0)
    public void skipKnownDefectsUnlessRequested() {
        boolean run = Config.optional(Config.RUN_KNOWN_DEFECTS).map(Boolean::parseBoolean).orElse(false);
        if (!run) {
            throw new TestAbortedException("Skipped: known defect on the target environment"
                    + " (see docs/API.md, 'Live verification'). Set RUN_KNOWN_DEFECTS=true to run it.");
        }
    }

    /** Deletes whatever the scenario created, then attaches the HTTP traffic to the report. */
    @After
    public void cleanUpAndAttachEvidence(Scenario scenario) {
        List<String> outcomes = context.cleanup().runAll();
        if (!outcomes.isEmpty()) {
            scenario.attach(String.join("\n", outcomes), "text/plain", "Cleanup");
        }
        if (!httpLog.isEmpty()) {
            scenario.attach(httpLog.render(), "text/plain", "HTTP exchanges (secrets masked)");
        }
    }
}
