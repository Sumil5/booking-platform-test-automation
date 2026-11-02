@regression @rooms
Feature: Rooms
  Anyone can browse rooms. Only an authenticated admin can create, update or delete them.
  Every scenario creates its own room, so it does not depend on the shared seed data.

  @smoke
  Scenario: The room list returns complete room records
    Given a room exists
    When I request the list of rooms
    Then the response status should be 200
    And the room list should include my room
    And every room in the list should have an id, name, type and price

  @smoke
  Scenario: Admin creates a room
    When I create a room of type "Family" priced at 180
    Then the response should be successful
    And the room should be listed with the details I sent

  Scenario: A room can be fetched by its id
    Given a room exists
    When I request the room by its id
    Then the response status should be 200
    And the room response should match the room I created

  Scenario: Admin updates a room
    Given a room exists
    When I update the room to type "Suite" priced at 275
    Then the response should be successful
    And fetching the room should show type "Suite" and price 275

  Scenario: Admin deletes a room
    Given a room exists
    When I delete the room
    Then the response should be successful
    And the room should no longer exist

  Scenario: A booked room is not offered for the booked dates
    Given a room exists
    And the room has a booking
    When I search for rooms available on the booking's dates
    Then the response status should be 200
    And the room list should not include my room

  Scenario: A booked room is still offered for other dates
    Given a room exists
    And the room has a booking
    When I search for rooms available on dates after the booking
    Then the response status should be 200
    And the room list should include my room

  @negative
  Scenario Outline: Room creation rejects an invalid <field>
    When I create a room with "<field>" set to <value>
    Then the response status should be 400
    And the error messages should include "<error>"

    Examples:
      | field     | value       | error                                                                       |
      | roomName  | ""          | Room name must be set                                                       |
      | type      | "Penthouse" | Type can only contain the room options Single, Double, Twin, Family or Suite |
      | type      | <omit>      | Type must be set                                                            |
      | roomPrice | 0           | must be greater than or equal to 1                                          |
      | roomPrice | 1000        | must be less than or equal to 999                                           |

  @negative
  Scenario: Creating a room without a token is rejected
    When I create a room with no token
    Then the response status should be 401
    And the error messages should include "Authentication required"

  @negative
  Scenario Outline: Changing a room with <kind> token is rejected
    Given a room exists
    When I try to <action> with <kind> token
    Then the request should be rejected as unauthorised
    And the room should be unchanged

    Examples:
      | action          | kind       |
      | update the room | no         |
      | update the room | an invalid |
      | update the room | a tampered |
      | delete the room | no         |
      | delete the room | an invalid |
      | delete the room | a tampered |

  @negative
  Scenario: Fetching a room that does not exist returns 404
    When I request a room that does not exist
    Then the response status should be 404

  @negative
  Scenario: Deleting a room that does not exist returns 404
    When I delete a room that does not exist
    Then the response status should be 404
