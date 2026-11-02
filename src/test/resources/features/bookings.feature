@regression @bookings
Feature: Bookings
  Guests book a room without logging in. Admins read, change and cancel bookings.
  Every scenario books a room it created itself, so dates never clash with other users.

  # @known-defect: on the live site every /{id} path (get, update, delete, mark-read)
  # returns 500. These scenarios describe correct behaviour and are skipped by default.
  # See docs/API.md "Live verification". Run them with RUN_KNOWN_DEFECTS=true.

  Background:
    Given a room exists

  @smoke
  Scenario: Guest books a room
    When I book the room for 3 nights
    Then the response status should be 201
    And the booking response should contain a booking id
    And the booking response should match the booking I sent

  @smoke @known-defect
  Scenario: Admin fetches a booking by its id
    Given the room has a booking
    When I request the booking by its id
    Then the response status should be 200
    And the booking response should match the booking I sent

  Scenario: Admin lists the bookings for a room
    Given the room has a booking
    When I list the bookings for the room
    Then the response status should be 200
    And the booking list should contain my booking
    And every booking in the list should be for my room

  @known-defect
  Scenario: Admin changes the guest name on a booking
    Given the room has a booking
    When I change the guest name on the booking to "Grace" "Hopper"
    Then the response status should be 200
    And fetching the booking should show guest "Grace" "Hopper"

  @known-defect
  Scenario: Admin cancels a booking
    Given the room has a booking
    When I delete the booking
    Then the response should be successful
    And the booking should no longer exist

  @negative
  Scenario: A booking that overlaps an existing one is rejected
    Given the room has a booking
    When I book the room again for the same dates
    Then the response status should be 409

  @negative
  Scenario: A booking with checkout before checkin is rejected
    When I book the room with a checkout date before the checkin date
    Then the response status should be 409

  @negative
  Scenario Outline: Booking rejects an invalid <field>
    When I book the room with "<field>" set to <value>
    Then the response status should be 400
    And the error messages should include "<error>"

    Examples:
      | field                | value                    | error                                |
      | firstname            | ""                       | Firstname should not be blank        |
      | firstname            | "Al"                     | size must be between 3 and 18        |
      | firstname            | "Bartholomew-Alexander"  | size must be between 3 and 18        |
      | lastname             | ""                       | Lastname should not be blank         |
      | lastname             | "Li"                     | size must be between 3 and 30        |
      | roomid               | 0                        | must be greater than or equal to 1   |
      | bookingdates.checkin | null                     | must not be null                     |
      | email                | "not-an-email"           | must be a well-formed email address  |
      | phone                | "123"                    | size must be between 11 and 21       |

  @negative
  Scenario: Listing bookings without a token is rejected
    When I list the bookings for the room with no token
    Then the response status should be 401
    And the error messages should include "Authentication required"

  @negative @known-defect
  Scenario Outline: Fetching a booking with <kind> token is forbidden
    Given the room has a booking
    When I try to fetch the booking with <kind> token
    Then the response status should be 403

    Examples:
      | kind       |
      | an invalid |
      | a tampered |

  @negative @known-defect
  Scenario Outline: Admin booking operations with <kind> token are rejected
    Given the room has a booking
    When I try to <action> with <kind> token
    Then the request should be rejected as unauthorised
    And the booking should be unchanged

    Examples:
      | action             | kind       |
      | fetch the booking  | no         |
      | update the booking | no         |
      | update the booking | an invalid |
      | update the booking | a tampered |
      | delete the booking | no         |
      | delete the booking | an invalid |
      | delete the booking | a tampered |

  @negative @known-defect
  Scenario: Fetching a booking that does not exist returns 404
    When I request a booking that does not exist
    Then the response status should be 404

  @negative @known-defect
  Scenario: Cancelling a booking that does not exist returns 404
    When I delete a booking that does not exist
    Then the response status should be 404
