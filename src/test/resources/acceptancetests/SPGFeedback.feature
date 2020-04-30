@Census @Acceptance @SPG @Feedback
Feature: SPG Outcome Tests

  Scenario Outline: As a Gateway I can receive an SPG outcome which will provide feedback to tm
    Given a job has been created in TM with case id "<caseId>"
    And tm sends a "<Type>" outcome
    Then a "<input>" feedback message is sent to tm
    And "<output>" is acknowledged by tm
    Examples:
      | caseId                               | Type               | input             | output           |
      | 8dd42be3-09e6-488e-b4e2-0f14259acb9e | CANCEL_FEEDBACK    | COMET_CANCEL_SENT | COMET_CANCEL_ACK |
      | 8dd42be3-09e6-488e-b4e2-0f14259acb9e | DELIVERED_FEEDBACK | COMET_UPDATE_SENT | COMET_UPDATE_ACK |