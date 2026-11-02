@regression @auth
Feature: Authentication
  Admin operations need a session token. POST /api/auth/login issues it, and clients
  send it back as a "token" cookie. POST /api/auth/validate checks whether a token is live.

  @smoke
  Scenario: Admin logs in with valid credentials
    When I log in with the configured admin credentials
    Then the response status should be 200
    And the response field "token" should be a non-empty string

  @smoke
  Scenario: A token issued at login is accepted
    When I validate a valid token
    Then the response status should be 200
    And the response field "valid" should be true

  @negative
  Scenario: Login with the wrong password is rejected
    When I log in as the admin user with a wrong password
    Then the response status should be 401
    And the error messages should include "Invalid credentials"

  @negative
  Scenario: Login as an unknown user is rejected
    When I log in as an unknown user
    Then the response status should be 401
    And the error messages should include "Invalid credentials"

  @negative
  Scenario: Login with empty credentials is rejected
    When I log in with an empty username and password
    Then the response status should be 401

  @negative
  Scenario Outline: Token validation rejects <kind> token
    When I validate <kind> token
    Then the response status should be <status>
    And the error messages should include "<error>"

    Examples:
      | kind       | status | error             |
      | no         | 401    | No token provided |
      | an invalid | 403    | Invalid token     |
      | a tampered | 403    | Invalid token     |
