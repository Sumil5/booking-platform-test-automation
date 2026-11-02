package com.rbp.api.steps;

import com.rbp.api.context.ScenarioContext;
import com.rbp.api.support.ResponseBodies;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {

    private final ScenarioContext context;

    public CommonSteps(ScenarioContext context) {
        this.context = context;
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expected) {
        Response response = context.lastResponse();
        assertThat(response.statusCode())
                .as("HTTP status. Body: %s", response.asString())
                .isEqualTo(expected);
    }

    @Then("the response should be successful")
    public void theResponseShouldBeSuccessful() {
        Response response = context.lastResponse();
        assertThat(response.statusCode())
                .as("HTTP status should be 2xx. Body: %s", response.asString())
                .isBetween(200, 299);
    }

    /**
     * 401 or 403. The platform answers 401 when the Next.js layer sees no token, and 403
     * when the Spring service rejects one (see docs/API.md). Both are correct refusals.
     * Scenarios that hit a path with a known single answer assert that exact status.
     */
    @Then("the request should be rejected as unauthorised")
    public void theRequestShouldBeRejectedAsUnauthorised() {
        Response response = context.lastResponse();
        assertThat(response.statusCode())
                .as("HTTP status should be 401 or 403. Body: %s", response.asString())
                .isIn(401, 403);
    }

    @Then("the error messages should include {string}")
    public void theErrorMessagesShouldInclude(String expected) {
        assertThat(ResponseBodies.errorMessages(context.lastResponse()))
                .as("error messages in %s", context.lastResponse().asString())
                .contains(expected);
    }

    @Then("the response field {string} should be a non-empty string")
    public void theResponseFieldShouldBeANonEmptyString(String field) {
        assertThat(ResponseBodies.json(context.lastResponse()).path(field).asText())
                .as("field '%s'", field)
                .isNotBlank();
    }

    @Then("the response field {string} should be true")
    public void theResponseFieldShouldBeTrue(String field) {
        assertThat(ResponseBodies.json(context.lastResponse()).path(field).asBoolean(false))
                .as("field '%s' in %s", field, context.lastResponse().asString())
                .isTrue();
    }
}
