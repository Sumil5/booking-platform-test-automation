package com.rbp.api.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbp.api.client.MessageClient;
import com.rbp.api.context.ScenarioContext;
import com.rbp.api.data.PayloadMutator;
import com.rbp.api.data.TestDataFactory;
import com.rbp.api.model.MessagePayload;
import com.rbp.api.support.Fixtures;
import com.rbp.api.support.ResponseBodies;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageSteps {

    private final ScenarioContext context;
    private final MessageClient messages;
    private final Fixtures fixtures;

    public MessageSteps(ScenarioContext context, MessageClient messages, Fixtures fixtures) {
        this.context = context;
        this.messages = messages;
        this.fixtures = fixtures;
    }

    @Given("a contact message has been sent")
    public void aContactMessageHasBeenSent() {
        Fixtures.requireSuccess(fixtures.sendMessage(TestDataFactory.message()), "sending a contact message");
    }

    @When("I send a contact message")
    public void iSendAContactMessage() {
        context.lastResponse(fixtures.sendMessage(TestDataFactory.message()));
    }

    @When("I send a contact message with {string} set to {}")
    public void iSendAContactMessageWithFieldSetTo(String field, String jsonValue) {
        MessagePayload valid = TestDataFactory.message();
        context.message(valid);
        Response response = messages.create(PayloadMutator.with(valid, field, jsonValue));
        context.lastResponse(response);
        if (Fixtures.isSuccess(response)) {
            // Accepted despite invalid data: find it and clean it up anyway.
            fixtures.findMessageSummary(valid.subject()).ifPresent(m -> {
                int id = m.path("id").asInt();
                context.cleanup().register("delete message " + id, () -> messages.delete(id, Fixtures.adminToken()));
            });
        }
    }

    @When("I open the message by its id")
    public void iOpenTheMessageByItsId() {
        context.lastResponse(messages.get(context.messageId()));
    }

    @When("I request a message that does not exist")
    public void iRequestAMessageThatDoesNotExist() {
        context.lastResponse(messages.get(Fixtures.NON_EXISTENT_ID));
    }

    @When("I mark the message as read")
    public void iMarkTheMessageAsRead() {
        context.lastResponse(messages.markRead(context.messageId(), Fixtures.adminToken()));
    }

    @When("I delete the message")
    public void iDeleteTheMessage() {
        context.lastResponse(messages.delete(context.messageId(), Fixtures.adminToken()));
    }

    @When("I request the unread message count")
    public void iRequestTheUnreadMessageCount() {
        context.lastResponse(messages.count());
    }

    @Then("the message should be in the inbox as {word}")
    public void theMessageShouldBeInTheInboxAs(String state) {
        boolean expectRead = switch (state) {
            case "read" -> true;
            case "unread" -> false;
            default -> throw new IllegalArgumentException("Expected 'read' or 'unread', got " + state);
        };
        JsonNode summary = fixtures.findMessageSummary(context.message().subject())
                .orElseThrow(() -> new AssertionError("Message not found in GET /api/message"));
        assertThat(summary.path("id").asInt()).isEqualTo(context.messageId());
        assertThat(summary.path("name").asText()).isEqualTo(context.message().name());
        assertThat(summary.path("read").asBoolean()).as("read flag").isEqualTo(expectRead);
    }

    @Then("the message should match what was sent")
    public void theMessageShouldMatchWhatWasSent() {
        JsonNode body = ResponseBodies.json(context.lastResponse());
        MessagePayload sent = context.message();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(body.path("messageid").asInt()).as("messageid").isEqualTo(context.messageId());
        softly.assertThat(body.path("name").asText()).as("name").isEqualTo(sent.name());
        softly.assertThat(body.path("email").asText()).as("email").isEqualTo(sent.email());
        softly.assertThat(body.path("phone").asText()).as("phone").isEqualTo(sent.phone());
        softly.assertThat(body.path("subject").asText()).as("subject").isEqualTo(sent.subject());
        softly.assertThat(body.path("description").asText()).as("description").isEqualTo(sent.description());
        softly.assertAll();
    }

    @Then("the unread count should be at least {int}")
    public void theUnreadCountShouldBeAtLeast(int minimum) {
        JsonNode count = ResponseBodies.json(context.lastResponse()).path("count");
        assertThat(count.isInt()).as("'count' is an integer in %s", context.lastResponse().asString()).isTrue();
        assertThat(count.asInt()).isGreaterThanOrEqualTo(minimum);
    }

    @Then("the message should no longer exist")
    public void theMessageShouldNoLongerExist() {
        assertThat(messages.get(context.messageId()).statusCode()).isEqualTo(404);
    }
}
