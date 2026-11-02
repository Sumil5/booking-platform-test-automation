package com.rbp.api.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbp.api.auth.TokenVariant;
import com.rbp.api.client.BookingClient;
import com.rbp.api.context.ScenarioContext;
import com.rbp.api.data.PayloadMutator;
import com.rbp.api.data.TestDataFactory;
import com.rbp.api.model.BookingDates;
import com.rbp.api.model.BookingPayload;
import com.rbp.api.support.Fixtures;
import com.rbp.api.support.ResponseBodies;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingSteps {

    private final ScenarioContext context;
    private final BookingClient bookings;
    private final Fixtures fixtures;

    public BookingSteps(ScenarioContext context, BookingClient bookings, Fixtures fixtures) {
        this.context = context;
        this.bookings = bookings;
        this.fixtures = fixtures;
    }

    // ---------- Given ----------

    @Given("the room has a booking")
    public void theRoomHasABooking() {
        Response response = fixtures.createBooking(TestDataFactory.booking(context.roomId(), 3));
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Test data setup failed: creating a booking returned HTTP "
                    + response.statusCode() + " " + response.asString());
        }
    }

    // ---------- When ----------

    @When("I book the room for {int} nights")
    public void iBookTheRoomForNights(int nights) {
        context.lastResponse(fixtures.createBooking(TestDataFactory.booking(context.roomId(), nights)));
    }

    @When("I book the room again for the same dates")
    public void iBookTheRoomAgainForTheSameDates() {
        BookingPayload overlapping = TestDataFactory.booking(context.roomId(), 1)
                .withDates(context.booking().bookingdates());
        context.lastResponse(createExpectingFailure(overlapping));
    }

    @When("I book the room with a checkout date before the checkin date")
    public void iBookTheRoomWithACheckoutDateBeforeTheCheckinDate() {
        BookingDates valid = TestDataFactory.futureDates(3);
        BookingPayload reversed = TestDataFactory.booking(context.roomId(), 3)
                .withDates(new BookingDates(valid.checkout(), valid.checkin()));
        context.lastResponse(createExpectingFailure(reversed));
    }

    @When("I book the room with {string} set to {}")
    public void iBookTheRoomWithFieldSetTo(String field, String jsonValue) {
        BookingPayload valid = TestDataFactory.booking(context.roomId(), 2);
        context.lastResponse(createExpectingFailure(PayloadMutator.with(valid, field, jsonValue)));
    }

    @When("I request the booking by its id")
    public void iRequestTheBookingByItsId() {
        context.lastResponse(bookings.get(context.bookingId(), Fixtures.adminToken()));
    }

    @When("I list the bookings for the room")
    public void iListTheBookingsForTheRoom() {
        context.lastResponse(bookings.listForRoom(context.roomId(), Fixtures.adminToken()));
    }

    @When("I list the bookings for the room with {token} token")
    public void iListTheBookingsForTheRoomWithToken(TokenVariant token) {
        context.lastResponse(bookings.listForRoom(context.roomId(), token.resolve()));
    }

    @When("I change the guest name on the booking to {string} {string}")
    public void iChangeTheGuestNameOnTheBookingTo(String firstname, String lastname) {
        BookingPayload updated = context.booking().withNames(firstname, lastname);
        context.lastResponse(bookings.update(context.bookingId(), updated, Fixtures.adminToken()));
        context.booking(context.bookingId(), updated);
    }

    @When("I delete the booking")
    public void iDeleteTheBooking() {
        context.lastResponse(bookings.delete(context.bookingId(), Fixtures.adminToken()));
    }

    @When("I request a booking that does not exist")
    public void iRequestABookingThatDoesNotExist() {
        context.lastResponse(bookings.get(Fixtures.NON_EXISTENT_ID, Fixtures.adminToken()));
    }

    @When("I delete a booking that does not exist")
    public void iDeleteABookingThatDoesNotExist() {
        context.lastResponse(bookings.delete(Fixtures.NON_EXISTENT_ID, Fixtures.adminToken()));
    }

    // ---------- Then ----------

    @Then("the booking response should match the booking I sent")
    public void theBookingResponseShouldMatchTheBookingISent() {
        assertBookingMatches(ResponseBodies.booking(context.lastResponse()), context.booking());
    }

    @Then("the booking response should contain a booking id")
    public void theBookingResponseShouldContainABookingId() {
        assertThat(ResponseBodies.bookingId(context.lastResponse())).isPositive();
    }

    @Then("the booking list should contain my booking")
    public void theBookingListShouldContainMyBooking() {
        JsonNode listed = Fixtures.findInList(context.lastResponse(), "bookings",
                        b -> b.path("bookingid").asInt() == context.bookingId())
                .orElseThrow(() -> new AssertionError("Booking " + context.bookingId()
                        + " not in " + context.lastResponse().asString()));
        assertBookingMatches(listed, context.booking());
    }

    @Then("every booking in the list should be for my room")
    public void everyBookingInTheListShouldBeForMyRoom() {
        List<Integer> roomIds = new ArrayList<>();
        ResponseBodies.json(context.lastResponse()).path("bookings").forEach(b -> roomIds.add(b.path("roomid").asInt()));
        assertThat(roomIds).isNotEmpty().containsOnly(context.roomId());
    }

    @Then("fetching the booking should show guest {string} {string}")
    public void fetchingTheBookingShouldShowGuest(String firstname, String lastname) {
        Response fetched = bookings.get(context.bookingId(), Fixtures.adminToken());
        assertThat(fetched.statusCode()).isEqualTo(200);
        JsonNode body = ResponseBodies.booking(fetched);
        assertThat(body.path("firstname").asText()).isEqualTo(firstname);
        assertThat(body.path("lastname").asText()).isEqualTo(lastname);
        assertBookingMatches(body, context.booking());
    }

    @Then("the booking should be unchanged")
    public void theBookingShouldBeUnchanged() {
        Response fetched = bookings.get(context.bookingId(), Fixtures.adminToken());
        assertThat(fetched.statusCode()).as("booking %s still exists", context.bookingId()).isEqualTo(200);
        assertBookingMatches(ResponseBodies.booking(fetched), context.booking());
    }

    @Then("the booking should no longer exist")
    public void theBookingShouldNoLongerExist() {
        assertThat(bookings.get(context.bookingId(), Fixtures.adminToken()).statusCode()).isEqualTo(404);
    }

    /** Sends a booking the test expects the API to reject; if it is accepted anyway, it still gets cleaned up. */
    private Response createExpectingFailure(Object body) {
        Response response = bookings.create(body);
        if (response.statusCode() == 201) {
            int id = ResponseBodies.bookingId(response);
            context.cleanup().register("delete booking " + id, () -> bookings.delete(id, Fixtures.adminToken()));
        }
        return response;
    }

    private static void assertBookingMatches(JsonNode actual, BookingPayload expected) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actual.path("roomid").asInt()).as("roomid").isEqualTo(expected.roomid());
        softly.assertThat(actual.path("firstname").asText()).as("firstname").isEqualTo(expected.firstname());
        softly.assertThat(actual.path("lastname").asText()).as("lastname").isEqualTo(expected.lastname());
        softly.assertThat(actual.path("depositpaid").asBoolean()).as("depositpaid").isEqualTo(expected.depositpaid());
        softly.assertThat(actual.path("bookingdates").path("checkin").asText()).as("checkin")
                .isEqualTo(expected.bookingdates().checkin().toString());
        softly.assertThat(actual.path("bookingdates").path("checkout").asText()).as("checkout")
                .isEqualTo(expected.bookingdates().checkout().toString());
        softly.assertAll();
    }
}
