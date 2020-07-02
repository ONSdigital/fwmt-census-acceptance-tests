package uk.gov.ons.census.fwmt.tests.acceptance.steps.inbound.cancel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.ons.census.fwmt.tests.acceptance.steps.inbound.common.CommonUtils.testBucket;

import java.net.URISyntaxException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;
import uk.gov.ons.census.fwmt.events.utils.GatewayEventMonitor;
import uk.gov.ons.census.fwmt.tests.acceptance.steps.inbound.common.CommonUtils;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.QueueClient;

@Slf4j
public class CancelSteps {

  @Autowired
  private CommonUtils commonUtils;

  @Autowired
  private QueueClient queueUtils;

  @Autowired
  private GatewayEventMonitor gatewayEventMonitor;

  private GatewayEventDTO event_COMET_CANCEL_PRE_SENDING;

  private String spgCancel;
  private String ceEstabCancel;
  private String ceUnitCancel;

  private static final String RM_CANCEL_REQUEST_RECEIVED = "RM_CANCEL_REQUEST_RECEIVED";

  private static final String COMET_CANCEL_ACK = "COMET_CANCEL_ACK";
  private static final String ROUTING_FAILED = "ROUTING_FAILED";

  private static final String COMET_CANCEL_PRE_SENDING = "COMET_CANCEL_PRE_SENDING";

  @Before
  public void setup() throws Exception {
    commonUtils.setup();
    spgCancel = Resources.toString(Resources.getResource("files/input/spg/spgCancel.json"), Charsets.UTF_8);
    ceEstabCancel = Resources.toString(Resources.getResource("files/input/ce/ceEstabCancel.json"), Charsets.UTF_8);
    ceUnitCancel = Resources.toString(Resources.getResource("files/input/ce/ceUnitCancel.json"), Charsets.UTF_8);

  }

  @After
  public void clearDown() throws Exception {
    commonUtils.clearDown();
  }

  @And("RM sends a cancel case request for the case")
  public void rmSendsCancel() throws URISyntaxException {
    String caseId = testBucket.get("caseId");
    String type = testBucket.get("type");

    JSONObject json = new JSONObject(getCreateRMJson());
    json.remove("caseId");
    json.put("caseId", caseId);

    json.remove("addressLevel");
    if ("Estab".equals(type) || "CE Est".equals(type) || "CE Site".equals(type)) {
      json.put("addressLevel", "E");
    }
    if ("Unit".equals(type) || "CE Unit".equals(type)) {
      json.put("addressLevel", "U");
    }

    String request = json.toString(4);
    log.info("Request = " + request);
    queueUtils.sendToRMFieldQueue(request, "cancel");
  }

  @When("Gateway receives a cancel message for the case")
  public void gatewayReceivesTheMessage() {
    String caseId = testBucket.get("caseId");
    boolean hasBeenTriggered = gatewayEventMonitor.hasEventTriggered(caseId, RM_CANCEL_REQUEST_RECEIVED, 10000L);
    assertThat(hasBeenTriggered).isTrue();
  }

  @Then("it will Cancel the job with with the correct TM Action {string}")
  public void confirmTmAction(String expectedTmAction) {
    String caseId = testBucket.get("caseId");
    boolean hasBeenTriggered = gatewayEventMonitor.hasEventTriggered(caseId, COMET_CANCEL_PRE_SENDING, 10000L);
    assertThat(hasBeenTriggered).isTrue();
    List<GatewayEventDTO> events = gatewayEventMonitor.getEventsForEventType(COMET_CANCEL_PRE_SENDING, 1);
    event_COMET_CANCEL_PRE_SENDING = events.get(0);
    String actualTmAction = event_COMET_CANCEL_PRE_SENDING.getMetadata().get("TM Action");
    assertEquals("TM Actions created for TM", expectedTmAction, actualTmAction);
  }

  @Then("the cancel job is acknowledged by TM")
  public void the_cancel_job_is_acknowledged_by_tm() {
    String caseId = testBucket.get("caseId");
    boolean hasBeenTriggered = gatewayEventMonitor.hasEventTriggered(caseId, COMET_CANCEL_ACK, 10000L);
    assertTrue(hasBeenTriggered);
  }

  @Then("the cancel job should fail")
  public void the_cancel_job_should_fail() {
    String caseId = testBucket.get("caseId");
    boolean hasBeenTriggered = gatewayEventMonitor.hasErrorEventTriggered(caseId, ROUTING_FAILED, 10000L);
    assertTrue(hasBeenTriggered);
  }

  @Given("RM sends a cancel case request")
  public void rm_sends_a_cancel_case_request() throws URISyntaxException {
    JSONObject json = new JSONObject(spgCancel);
    json.remove("addressLevel");
    json.put("addressLevel", "E");
    

    String request = json.toString(4);
    log.info("Request = " + request);
    queueUtils.sendToRMFieldQueue(request, "cancel");
  }

  private String getCreateRMJson() {
    String type = testBucket.get("type");
    String survey = testBucket.get("survey");

    switch (type) {
      case "Estab":
      case "Unit":
        return spgCancel;
      case "CE Est":
      case "CE Site":
        return ceEstabCancel;
      case "CE Unit":
        return ceUnitCancel;
      default:
        throw new RuntimeException("Incorrect survey " + survey + " and type " + type);
    }
  }
}