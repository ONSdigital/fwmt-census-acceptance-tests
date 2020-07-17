package uk.gov.ons.census.fwmt.tests.acceptance.steps.addresscheck;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.census.ffa.storage.utils.StorageUtils;
import uk.gov.ons.census.fwmt.common.data.modelcase.ModelCase;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;
import uk.gov.ons.census.fwmt.events.utils.GatewayEventMonitor;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.CSVSerivceUtils;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.QueueClient;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.TMMockUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class AddressCheckServiceTestSteps {

  public static final String CSV_ADDRESS_CHECK_REQUEST_EXTRACTED = "CSV_ADDRESS_CHECK_REQUEST_EXTRACTED";
  public static final String COMET_CREATE_ACK = "COMET_CREATE_ACK";

  @Autowired
  private TMMockUtils tmMockUtils;

  @Autowired
  private QueueClient queueUtils;

  @Autowired
  private CSVSerivceUtils csvSerivceUtils;

  @Autowired
  private StorageUtils storageUtils;

  @Value("${service.csvservice.gcpBucket.directory}")
  private String directory;

  private GatewayEventMonitor gatewayEventMonitor = new GatewayEventMonitor();

  @Value("${service.rabbit.url}")
  private String rabbitLocation;

  @Value("${service.rabbit.username}")
  private String rabbitUsername;

  @Value("${service.rabbit.password}")
  private String rabbitPassword;

  private String caseId;

  @Before
  public void setup() throws IOException, TimeoutException, URISyntaxException {
    tmMockUtils.enableRequestRecorder();
    tmMockUtils.resetMock();
    queueUtils.clearQueues();

    File jobFile = new File(getClass().getClassLoader().getResource("files/csv/AC15_07_94.csv").getFile());
    storageUtils.move(jobFile.toURI(), URI.create(directory + jobFile.getName()));

    File lookupFile = new File(getClass().getClassLoader().getResource("files/csv/addressLookup.csv").getFile());
    storageUtils.move(lookupFile.toURI(), URI.create(directory + lookupFile.getName()));

    gatewayEventMonitor = new GatewayEventMonitor();
    gatewayEventMonitor.enableEventMonitor(rabbitLocation, rabbitUsername, rabbitPassword);
  }

  @After
  public void tearDownGatewayEventMonitor() throws IOException {
    gatewayEventMonitor.tearDownGatewayEventMonitor();
    tmMockUtils.disableRequestRecorder();

    List<URI> filesToDelete = storageUtils.getFilenamesInFolder(URI.create(directory));
    for (URI uri : filesToDelete){
      storageUtils.delete(uri);
    }
  }

  @Given("the Gateway receives a CSV Address Check")
  public void theGatewayReceivesACSVAddressCheck() {
    csvSerivceUtils.ingestAddressCheckFile();

    Collection<GatewayEventDTO> message;

    csvSerivceUtils.enableAddressCheckCsvService();

    message = gatewayEventMonitor.grabEventsTriggered(CSV_ADDRESS_CHECK_REQUEST_EXTRACTED, 1, 10000L);

    for (GatewayEventDTO retrieveCaseId : message) {
      caseId = retrieveCaseId.getCaseId();
    }

    boolean hasBeenTriggered = gatewayEventMonitor.hasEventTriggered(caseId, CSV_ADDRESS_CHECK_REQUEST_EXTRACTED, 10000L);
    assertThat(hasBeenTriggered).isTrue();
  }

  @Then("a new case id for job containing postcode {string} is created in TM")
  public void aNewCaseIdForJobContainingPostcodeIsCreatedInTM(String postcode) {
    boolean hasBeenTriggered = gatewayEventMonitor.hasEventTriggered(caseId, COMET_CREATE_ACK, 10000L);
    assertThat(hasBeenTriggered).isTrue();

    ModelCase modelCase = tmMockUtils.getCaseById(caseId);
    assertEquals(caseId, modelCase.getId().toString());
    assertEquals(postcode, modelCase.getAddress().getPostcode());
  }
}