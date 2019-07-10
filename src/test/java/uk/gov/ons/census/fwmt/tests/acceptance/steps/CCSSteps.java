package uk.gov.ons.census.fwmt.tests.acceptance.steps;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import uk.gov.ons.census.fwmt.events.data.GatewayEventDTO;
import uk.gov.ons.census.fwmt.events.utils.GatewayEventMonitor;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.CSVSerivceUtils;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.QueueUtils;
import uk.gov.ons.census.fwmt.tests.acceptance.utils.TMMockUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeoutException;


@Slf4j
@PropertySource("classpath:application.properties")
public class CCSSteps {

    private static final String CANONICAL_CREATE_SENT = "Canonical - Action Create Sent";
    public static final String CSV_CCS_REQUEST_EXTRACTED = "CSV Service - CCS Request extracted";

    @Autowired
    private TMMockUtils tmMockUtils;

    @Autowired
    private QueueUtils queueUtils;

    @Autowired
    private CSVSerivceUtils csvSerivceUtils;

    private GatewayEventMonitor gatewayEventMonitor;

    @Value("${service.mocktm.url}")
    private String mockTmUrl;

    @Value("${service.rabbit.url}")
    private String rabbitLocation;

    @Before
    public void setup() throws IOException, TimeoutException, URISyntaxException {

        tmMockUtils.enableRequestRecorder();
        tmMockUtils.resetMock();
        queueUtils.clearQueues();

        gatewayEventMonitor = new GatewayEventMonitor();
        gatewayEventMonitor.enableEventMonitor(rabbitLocation);
    }

    @After
    public void tearDownGatewayEventMonitor() throws IOException, TimeoutException {
        gatewayEventMonitor.tearDownGatewayEventMonitor();
        tmMockUtils.disableRequestRecorder();
    }
    @Given("the Gateway receives a CSV CCS")
    public void theGatewayReceivesACSVCCSWithCaseID() throws IOException, InterruptedException, URISyntaxException {
        Collection<GatewayEventDTO> message;
        String caseId = null;
        int caseIdStart;
        csvSerivceUtils.enableCCSCsvService();

        message = gatewayEventMonitor.grabEventsTriggered("CSV Service - CCS Request extracted", 1, 10000L);

    }

    @Then("a new case with new case id for job containing postcode {string} is created in TM")
    public void aNewCaseWithNewCaseIdForJobContainingPostcodeIsCreatedInTM(String postcode) throws InterruptedException {
//        Thread.sleep(1000);
//        ModelCase modelCase = tmMockUtils.getCaseById("");
//        assertEquals(postcode, modelCase.getAddress().getPostcode());
    }
}
