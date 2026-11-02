@regression @messages
Feature: Contact messages
  Guests send messages through the public contact form. Admins read, mark and delete them.

  @smoke
  Scenario: Guest sends a contact message
    When I send a contact message
    Then the response should be successful
    And the message should be in the inbox as unread

  @smoke
  Scenario: A message can be opened by its id
    Given a contact message has been sent
    When I open the message by its id
    Then the response status should be 200
    And the message should match what was sent

  Scenario: Admin marks a message as read
    Given a contact message has been sent
    When I mark the message as read
    Then the response status should be 202
    And the message should be in the inbox as read

  Scenario: The unread count includes a new message
    Given a contact message has been sent
    When I request the unread message count
    Then the response status should be 200
    And the unread count should be at least 1

  Scenario: Admin deletes a message
    Given a contact message has been sent
    When I delete the message
    Then the response should be successful
    And the message should no longer exist

  @negative
  Scenario Outline: The contact form rejects an invalid <field>
    When I send a contact message with "<field>" set to <value>
    Then the response status should be 400
    And the error messages should include "<error>"

    Examples:
      | field       | value          | error                                           |
      | name        | ""             | Name may not be blank                           |
      | email       | "not-an-email" | must be a well-formed email address             |
      | email       | <omit>         | Email must be set                               |
      | phone       | "123"          | Phone must be between 11 and 21 characters.     |
      | subject     | "Hi"           | Subject must be between 5 and 100 characters.   |
      | description | "Too short"    | Message must be between 20 and 2000 characters. |

  @negative
  Scenario Outline: Changing a message with <kind> token is rejected
    Given a contact message has been sent
    When I try to <action> with <kind> token
    Then the request should be rejected as unauthorised
    And the message should be in the inbox as unread

    Examples:
      | action                   | kind       |
      | mark the message as read | no         |
      | mark the message as read | an invalid |
      | mark the message as read | a tampered |
      | delete the message       | no         |
      | delete the message       | an invalid |
      | delete the message       | a tampered |

  @negative
  Scenario: Opening a message that does not exist returns 404
    When I request a message that does not exist
    Then the response status should be 404
