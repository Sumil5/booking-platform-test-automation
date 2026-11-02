package com.rbp.api.hooks;

import com.rbp.api.context.ScenarioContext;
import com.rbp.api.support.HttpExchangeLog;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

import java.util.List;

public class Hooks {

    private final ScenarioContext context;
    private final HttpExchangeLog httpLog;

    public Hooks(ScenarioContext context, HttpExchangeLog httpLog) {
        this.context = context;
        this.httpLog = httpLog;
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
