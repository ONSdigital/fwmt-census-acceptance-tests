package uk.gov.ons.census.fwmt.tests.acceptance.steps.ccsinterview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.fwmt.events.utils.GatewayEventMonitor;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.QueueUtils;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.TMMockUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class OutcomeCCSInterviewSteps {

    @Autowired
    private TMMockUtils tmMockUtils;

    @Autowired
    private QueueUtils queueUtils;

    private GatewayEventMonitor gatewayEventMonitor = new GatewayEventMonitor();

    private String tmRequest = null;

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    private JsonNode tmRequestRootNode;

    private String caseId;

    @Value("${service.rabbit.url}")
    private String rabbitLocation;

    @Value("${service.rabbit.username}")
    private String rabbitUsername;

    @Value("${service.rabbit.password}")
    private String rabbitPassword;

    private String secondaryOutcome;

    public static final String OUTCOME_SENT_RM = "Outcome - Case Outcome Sent";

    private String actualMessage;

    private boolean qIdHasValue;

    private String resourcePath;

    @Before
    public void before() throws URISyntaxException {
        try {
            gatewayEventMonitor.enableEventMonitor(rabbitLocation, rabbitUsername, rabbitPassword);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Problem with setting up", e);
        }

        queueUtils.clearQueues();
    }

    @After
    public void after() throws IOException, TimeoutException, URISyntaxException {
        gatewayEventMonitor.tearDownGatewayEventMonitor();
    }

    private String createPathnameFromOutcomeName(String outcomeName) {
        String pathname = outcomeName.replaceAll("[^A-Za-z]+", "").toLowerCase();
        return pathname;
    }

    private String getTmRequest(String inputMessage) {
        try {
            String pathname = createPathnameFromOutcomeName(inputMessage);
            String message = Resources.toString(
                    Resources.getResource("files/outcome/" + resourcePath + "/" + pathname + "/tmrequest" + (qIdHasValue ? "-q" : "") + ".json"), Charsets.UTF_8);
            return message;
        } catch (IOException e) {
            throw new RuntimeException("Problem retrieving resource file", e);
        }
    }

    private String getExpectedCaseEvent(String so) {
        try {
            String pathname = createPathnameFromOutcomeName(so);
            String message = Resources.toString(
                    Resources.getResource("files/outcome/" + resourcePath + "/" + pathname + "/eventresponse" + (qIdHasValue ? "-q" : "") + ".json"), Charsets.UTF_8);
            return message;
        } catch (IOException e) {
            throw new RuntimeException("Problem retrieving resource file", e);
        }
    }

    private boolean compareCaseEventMessages(String so, String actualMessage) {
        try {
            String expectedCaseEvent = getExpectedCaseEvent(so);
            JsonNode expectedMessageRootNode = jsonObjectMapper.readTree(expectedCaseEvent);
            JsonNode actualMessageRootNode = jsonObjectMapper.readTree(actualMessage);

            boolean isEqual = expectedMessageRootNode.equals(actualMessageRootNode);
            if (!isEqual) {
                log.info("expected and actual caseEvents are not the same: \n expected:\n {} \n\n actual: \n {}", expectedCaseEvent, actualMessage);
            }
            return isEqual;

        } catch (IOException e) {
            throw new RuntimeException("Problem comparing 2 json files", e);
        }
    }

    private void readRequest(String inputMessage) {
        this.tmRequest = getTmRequest(inputMessage);
        this.secondaryOutcome = inputMessage;
        try {
            tmRequestRootNode = jsonObjectMapper.readTree(tmRequest);
        } catch (IOException e) {
            throw new RuntimeException("Problem parsing file", e);
        }
    }

    @Given("TM sends a {string} Census Case CCS interview Outcome to the Gateway with {string} and with the category {string}")
    public void tmSendsACensusCaseCCSPLOutcomeToTheGateway(String inputMessage, String primaryOutcome, String category) {
        String fileLocation = "";
        this.qIdHasValue = false;
        fileLocation = primaryOutcome.replaceAll("\\s+", "").replaceAll("/", "").toLowerCase()
                + "/" + category.replaceAll("\\s+", "").replaceAll("/", "").toLowerCase();
        resourcePath = "ccsinterview/" + fileLocation;
        readRequest(inputMessage);
    }

    @And("the Primary Outcome for CCS interview is {string}")
    public void thePrimaryOutcomeForCCSPLIs(String primaryOutcome) {
        JsonNode node = tmRequestRootNode.path("primaryOutcome");
        assertEquals(primaryOutcome, node.asText());
    }

    @And("the Secondary Outcome for CCS interview is {string}")
    public void theSecondaryOutcomeForCCSPLIs(String secondaryOutcome) {
        JsonNode node = tmRequestRootNode.path("secondaryOutcome");
        assertEquals(secondaryOutcome, node.asText());
    }

    @When("the Outcome Service processes the CCS interview message")
    public void theOutcomeServiceProcessTheCCSPLMessage() {
        JsonNode node = tmRequestRootNode.path("caseId");
        caseId = node.asText();
        int response = tmMockUtils.sendTMCCSIntResponseMessage(tmRequest, caseId);
        assertEquals(200, response);
    }

    @Then("the Outcome Service for the CCS interview should create a valid {string} for the correct {string}")
    public void theOutcomeServiceForTheCCSPLShouldCreateAValidForTheCorrect(String caseEvent, String routingKey) {
        gatewayEventMonitor.checkForEvent(caseId, OUTCOME_SENT_RM);
        try {
            actualMessage = queueUtils.getMessageOffQueueWithRoutingKey(caseEvent, routingKey);
            assertTrue(compareCaseEventMessages(secondaryOutcome, actualMessage));
        } catch (InterruptedException e) {
            throw new RuntimeException("Problem getting message", e);
        }
    }

    @Then("and of the correct CCS interview {string}")
    public void and_of_the_correct(String eventType) {
        try {
            JsonNode actualMessageRootNode = jsonObjectMapper.readTree(actualMessage);
            JsonNode node = actualMessageRootNode.path("event").path("type");
            assertEquals(eventType, node.asText());
        } catch (IOException e) {
            throw new RuntimeException("Problem parsing ", e);
        }
    }

}