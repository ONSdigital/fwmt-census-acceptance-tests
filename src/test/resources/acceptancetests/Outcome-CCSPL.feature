@Census @Acceptance @Outcome @CCSHouseholdPL
Feature: Non-valid household Outcome Tests

  Scenario Outline: As a Gateway I can receive a CCS PL outcome of cases from TM and create Census Events
    Given TM sends a "<InputMessage>" Census Case CCS PL Outcome to the Gateway with "<Primary Outcome>"
    And the Primary Outcome for CCS PL is "<Primary Outcome>"
    And the Secondary Outcome for CCS PL is "<InputMessage>"
    When the Outcome Service processes the CCS PL message
    Then the Outcome Service for the CCS PL should create a valid "<CaseEvent>" for the correct "<Topic>"
    And and of the correct CCS "<EventType>"

    Examples:
      | InputMessage                | Primary Outcome            | CaseEvent                   | Topic                     | EventType          |
      | Complete on paper (full)    | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Complete on paper (partial) | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Soft refusal                | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Hard refusal                | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Extraordinary refusal       | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Contact not needed          | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Derelict / uninhabitable    | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Potential Residential       | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Under construction          | Household                  | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Soft refusal                | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Hard refusal                | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Extraordinary refusal       | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Collect CE details          | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Contact not needed          | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Derelict / uninhabitable    | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Under construction          | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | CE Out of scope             | CE                         | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |
      | Non residential / business  | Non residential / business | Field.other | event.ccs.propertylisting | CCS_ADDRESS_LISTED |