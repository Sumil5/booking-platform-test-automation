package com.rbp.api.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbp.api.auth.TokenVariant;
import com.rbp.api.client.RoomClient;
import com.rbp.api.context.ScenarioContext;
import com.rbp.api.data.PayloadMutator;
import com.rbp.api.data.TestDataFactory;
import com.rbp.api.model.BookingPayload;
import com.rbp.api.model.RoomPayload;
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

public class RoomSteps {

    private final ScenarioContext context;
    private final RoomClient rooms;
    private final Fixtures fixtures;

    public RoomSteps(ScenarioContext context, RoomClient rooms, Fixtures fixtures) {
        this.context = context;
        this.rooms = rooms;
        this.fixtures = fixtures;
    }

    // ---------- Given ----------

    @Given("a room exists")
    public void aRoomExists() {
        Fixtures.requireSuccess(fixtures.createRoom(TestDataFactory.room()), "creating a room");
    }

    // ---------- When ----------

    @When("I create a room of type {string} priced at {int}")
    public void iCreateARoomOfTypePricedAt(String type, int price) {
        context.lastResponse(fixtures.createRoom(TestDataFactory.room(type, price)));
    }

    @When("I create a room with {string} set to {}")
    public void iCreateARoomWithFieldSetTo(String field, String jsonValue) {
        RoomPayload valid = TestDataFactory.room();
        context.room(valid);
        Response response = rooms.create(PayloadMutator.with(valid, field, jsonValue), Fixtures.adminToken());
        context.lastResponse(response);
        if (Fixtures.isSuccess(response)) {
            // The API accepted invalid data: clean up anyway so the shared site stays tidy.
            int id = fixtures.findRoomId(valid.roomName());
            context.cleanup().register("delete room " + id, () -> rooms.delete(id, Fixtures.adminToken()));
        }
    }

    @When("I create a room with {token} token")
    public void iCreateARoomWithToken(TokenVariant token) {
        RoomPayload payload = TestDataFactory.room();
        context.room(payload);
        context.lastResponse(rooms.create(payload, token.resolve()));
    }

    @When("I request the list of rooms")
    public void iRequestTheListOfRooms() {
        context.lastResponse(rooms.list());
    }

    @When("I request the room by its id")
    public void iRequestTheRoomByItsId() {
        context.lastResponse(rooms.get(context.roomId()));
    }

    @When("I request a room that does not exist")
    public void iRequestARoomThatDoesNotExist() {
        context.lastResponse(rooms.get(Fixtures.NON_EXISTENT_ID));
    }

    @When("I delete a room that does not exist")
    public void iDeleteARoomThatDoesNotExist() {
        context.lastResponse(rooms.delete(Fixtures.NON_EXISTENT_ID, Fixtures.adminToken()));
    }

    @When("I update the room to type {string} priced at {int}")
    public void iUpdateTheRoomToTypePricedAt(String type, int price) {
        RoomPayload updated = context.room().withType(type).withPrice(price);
        context.lastResponse(rooms.update(context.roomId(), updated, Fixtures.adminToken()));
        context.room(context.roomId(), updated);
    }

    @When("I delete the room")
    public void iDeleteTheRoom() {
        context.lastResponse(rooms.delete(context.roomId(), Fixtures.adminToken()));
    }

    @When("I search for rooms available on the booking's dates")
    public void iSearchForRoomsAvailableOnTheBookingsDates() {
        BookingPayload booking = context.booking();
        context.lastResponse(rooms.listAvailable(booking.bookingdates().checkin(), booking.bookingdates().checkout()));
    }

    @When("I search for rooms available on dates after the booking")
    public void iSearchForRoomsAvailableOnDatesAfterTheBooking() {
        BookingPayload booking = context.booking();
        var checkin = booking.bookingdates().checkout().plusDays(30);
        context.lastResponse(rooms.listAvailable(checkin, checkin.plusDays(2)));
    }

    // ---------- Then ----------

    @Then("every room in the list should have an id, name, type and price")
    public void everyRoomInTheListShouldHaveAnIdNameTypeAndPrice() {
        JsonNode list = ResponseBodies.json(context.lastResponse()).path("rooms");
        assertThat(list.isArray()).as("'rooms' array in %s", context.lastResponse().asString()).isTrue();
        assertThat(list.size()).as("number of rooms").isPositive();
        SoftAssertions softly = new SoftAssertions();
        for (JsonNode room : list) {
            softly.assertThat(room.path("roomid").asInt()).as("roomid of %s", room).isPositive();
            softly.assertThat(room.path("roomName").asText()).as("roomName of %s", room).isNotBlank();
            softly.assertThat(room.path("type").asText()).as("type of %s", room)
                    .isIn("Single", "Double", "Twin", "Family", "Suite");
            softly.assertThat(room.path("roomPrice").asInt()).as("roomPrice of %s", room).isBetween(1, 999);
        }
        softly.assertAll();
    }

    @Then("the room list should include my room")
    public void theRoomListShouldIncludeMyRoom() {
        assertThat(roomIdsIn(context.lastResponse())).contains(context.roomId());
    }

    @Then("the room list should not include my room")
    public void theRoomListShouldNotIncludeMyRoom() {
        assertThat(roomIdsIn(context.lastResponse())).doesNotContain(context.roomId());
    }

    @Then("the room should be listed with the details I sent")
    public void theRoomShouldBeListedWithTheDetailsISent() {
        String name = context.room().roomName();
        JsonNode listed = Fixtures.findInList(rooms.list(), "rooms", n -> name.equals(n.path("roomName").asText()))
                .orElseThrow(() -> new AssertionError("Room '" + name + "' is not in GET /api/room"));
        assertRoomMatches(listed, context.room());
    }

    @Then("the room response should match the room I created")
    public void theRoomResponseShouldMatchTheRoomICreated() {
        JsonNode body = ResponseBodies.json(context.lastResponse());
        assertThat(body.path("roomid").asInt()).isEqualTo(context.roomId());
        assertRoomMatches(body, context.room());
    }

    @Then("fetching the room should show type {string} and price {int}")
    public void fetchingTheRoomShouldShowTypeAndPrice(String type, int price) {
        Response fetched = rooms.get(context.roomId());
        assertThat(fetched.statusCode()).isEqualTo(200);
        JsonNode body = ResponseBodies.json(fetched);
        assertThat(body.path("type").asText()).isEqualTo(type);
        assertThat(body.path("roomPrice").asInt()).isEqualTo(price);
        assertThat(body.path("roomName").asText()).isEqualTo(context.room().roomName());
    }

    @Then("the room should be unchanged")
    public void theRoomShouldBeUnchanged() {
        Response fetched = rooms.get(context.roomId());
        assertThat(fetched.statusCode()).as("room %s still exists", context.roomId()).isEqualTo(200);
        assertRoomMatches(ResponseBodies.json(fetched), context.room());
    }

    @Then("the room should no longer exist")
    public void theRoomShouldNoLongerExist() {
        assertThat(rooms.get(context.roomId()).statusCode()).isEqualTo(404);
    }

    private static void assertRoomMatches(JsonNode actual, RoomPayload expected) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(actual.path("roomName").asText()).as("roomName").isEqualTo(expected.roomName());
        softly.assertThat(actual.path("type").asText()).as("type").isEqualTo(expected.type());
        softly.assertThat(actual.path("accessible").asBoolean()).as("accessible").isEqualTo(expected.accessible());
        softly.assertThat(actual.path("roomPrice").asInt()).as("roomPrice").isEqualTo(expected.roomPrice());
        softly.assertThat(actual.path("description").asText()).as("description").isEqualTo(expected.description());
        List<String> features = new ArrayList<>();
        actual.path("features").forEach(f -> features.add(f.asText()));
        softly.assertThat(features).as("features").containsExactlyInAnyOrderElementsOf(expected.features());
        softly.assertAll();
    }

    private static List<Integer> roomIdsIn(Response response) {
        List<Integer> ids = new ArrayList<>();
        ResponseBodies.json(response).path("rooms").forEach(r -> ids.add(r.path("roomid").asInt()));
        return ids;
    }
}
