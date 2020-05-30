package uk.gov.ons.census.fwmt.tests.acceptance.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.census.fwmt.common.data.modelcase.CasePause;
import uk.gov.ons.census.fwmt.common.data.modelcase.ModelCase;
import uk.gov.ons.census.fwmt.data.dto.MockMessage;
import uk.gov.ons.census.fwmt.tests.acceptance.exceptions.MockInaccessibleException;

import javax.xml.bind.JAXBContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
public final class TMMockUtils {

  @Value("${service.outcome.url}")
  private String outcomeServiceUrl;

  @Value("${service.outcome.household.endpoint}")
  private String householdOutcomeEndpoint;

  @Value("${service.outcome.CCSPL.endpoint}")
  private String ccsPLOutcomeEnpoint;

  @Value("${service.outcome.CCSInt.endpoint}")
  private String ccsIntOutcomeEnpoint;

  @Value("${service.outcome.SPG.endpoint}")
  private String spgOutcomeEndpoint;

  @Value("${service.outcome.SPGNewUnit.endpoint}")
  private String spgNewUnitOutcomeEndpoint;

  @Value("${service.outcome.SPGStandalone.endpoint}")
  private String spgStandaloneOutcomeEndpoint;

  @Value("${service.outcome.username}")
  private String outcomeServiceUsername;

  @Value("${service.outcome.password}")
  private String outcomeServicePassword;

  @Value("${service.mocktm.url}")
  private String mockTmUrl;

  @Value("${spring.datasource.url}")
  private String url;

  @Value("${spring.datasource.username}")
  private String username;

  @Value("${spring.datasource.password}")
  private String password;

  private RestTemplate restTemplate = new RestTemplate();

  private JAXBContext jaxbContext;

  public void resetMock() throws IOException {
    URL url = new URL(mockTmUrl + "/logger/reset");
    log.info("reset-mock_url:" + url.toString());
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    httpURLConnection.setRequestMethod("GET");
    if (httpURLConnection.getResponseCode() != 200) {
      throw new MockInaccessibleException("Failed : HTTP error code : " + httpURLConnection.getResponseCode());
    }
  }

  public MockMessage[] getMessages() {
    String url = mockTmUrl + "/logger/allMessages";
    log.info("allMessages-mock_url:" + url);
    return restTemplate.getForObject(url, MockMessage[].class);
  }

  public ModelCase getCaseById(String id) {
    String url = mockTmUrl + "/cases/" + id;
    log.info("getCaseById-mock_url:" + url);
    ResponseEntity<ModelCase> responseEntity;
    responseEntity = restTemplate.getForEntity(url, ModelCase.class);
    return responseEntity.getBody();
  }

  public CasePause getPauseCase(String id) {
    String url = mockTmUrl + "/cases/" + id + "/pause";
    log.info("getCancelCaseById-mock.url:" + url);
    ResponseEntity<CasePause> responseEntity;
    responseEntity = restTemplate.getForEntity(url, CasePause.class);
    return responseEntity.getBody();
  }

  public int sendTMResponseMessage(String data, String caseId) {
    HttpHeaders headers = createBasicAuthHeaders(outcomeServiceUsername, outcomeServicePassword);

    headers.setContentType(MediaType.APPLICATION_JSON);

    RestTemplate restTemplate = new RestTemplate();
    String postUrl = outcomeServiceUrl + householdOutcomeEndpoint + caseId;

    HttpEntity<String> post = new HttpEntity<>(data, headers);
    ResponseEntity<Void> response = restTemplate.exchange(postUrl, HttpMethod.POST, post, Void.class);

    return response.getStatusCode().value();
  }

  public int sendTMCCSPLResponseMessage(String data, String caseId) {
    HttpHeaders headers = createBasicAuthHeaders(outcomeServiceUsername, outcomeServicePassword);

    headers.setContentType(MediaType.APPLICATION_JSON);

    RestTemplate restTemplate = new RestTemplate();
    String postUrl = outcomeServiceUrl + ccsPLOutcomeEnpoint;

    HttpEntity<String> post = new HttpEntity<>(data, headers);
    ResponseEntity<Void> response = restTemplate.exchange(postUrl, HttpMethod.POST, post, Void.class);

    return response.getStatusCode().value();
  }

  public int sendTMCCSIntResponseMessage(String data, String caseId) {
    HttpHeaders headers = createBasicAuthHeaders(outcomeServiceUsername, outcomeServicePassword);

    headers.setContentType(MediaType.APPLICATION_JSON);

    RestTemplate restTemplate = new RestTemplate();
    String postUrl = outcomeServiceUrl + ccsIntOutcomeEnpoint + caseId;

    HttpEntity<String> post = new HttpEntity<>(data, headers);
    ResponseEntity<Void> response = restTemplate.exchange(postUrl, HttpMethod.POST, post, Void.class);

    return response.getStatusCode().value();
  }

  public int sendTMSPGResponseMessage(String data, String caseId) {
    HttpHeaders headers = createBasicAuthHeaders(outcomeServiceUsername, outcomeServicePassword);

    headers.setContentType(MediaType.APPLICATION_JSON);

    RestTemplate restTemplate = new RestTemplate();
    String postUrl = outcomeServiceUrl + spgOutcomeEndpoint + caseId;

    HttpEntity<String> post = new HttpEntity<>(data, headers);
    ResponseEntity<Void> response = restTemplate.exchange(postUrl, HttpMethod.POST, post, Void.class);

    return response.getStatusCode().value();
  }

  public int sendTMSPGNewStandaloneAddressResponseMessage(String data) {
    HttpHeaders headers = createBasicAuthHeaders(outcomeServiceUsername, outcomeServicePassword);

    headers.setContentType(MediaType.APPLICATION_JSON);

    RestTemplate restTemplate = new RestTemplate();
    String postUrl = outcomeServiceUrl + spgStandaloneOutcomeEndpoint;

    HttpEntity<String> post = new HttpEntity<>(data, headers);
    ResponseEntity<Void> response = restTemplate.exchange(postUrl, HttpMethod.POST, post, Void.class);

    return response.getStatusCode().value();
  }

  public int sendTMSPGNewUnitAddressResponseMessage(String data) {
    HttpHeaders headers = createBasicAuthHeaders(outcomeServiceUsername, outcomeServicePassword);

    headers.setContentType(MediaType.APPLICATION_JSON);

    RestTemplate restTemplate = new RestTemplate();
    String postUrl = outcomeServiceUrl + spgNewUnitOutcomeEndpoint;

    HttpEntity<String> post = new HttpEntity<>(data, headers);
    ResponseEntity<Void> response = restTemplate.exchange(postUrl, HttpMethod.POST, post, Void.class);

    return response.getStatusCode().value();
  }

  private HttpHeaders createBasicAuthHeaders(String username, String password) {
    HttpHeaders headers = new HttpHeaders();
    final String plainCreds = username + ":" + password;
    byte[] plainCredsBytes = plainCreds.getBytes();
    byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
    String base64Creds = new String(base64CredsBytes);
    headers.add("Authorization", "Basic " + base64Creds);
    return headers;
  }

  public void enableRequestRecorder() throws IOException {
    URL url = new URL(mockTmUrl + "/logger/enableRequestRecorder");
    log.info("enableRequestRecorder-mock_url:" + url.toString());
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    httpURLConnection.setRequestMethod("GET");
    if (httpURLConnection.getResponseCode() != 200) {
      throw new MockInaccessibleException("Failed : HTTP error code : " + httpURLConnection.getResponseCode());
    }
  }

  public void disableRequestRecorder() throws IOException {
    URL url = new URL(mockTmUrl + "/logger/disableRequestRecorder");
    log.info("disableRequestRecorder-mock_url:" + url.toString());
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    httpURLConnection.setRequestMethod("GET");
    if (httpURLConnection.getResponseCode() != 200) {
      throw new MockInaccessibleException("Failed : HTTP error code : " + httpURLConnection.getResponseCode());
    }
  }

  public void clearDownDatabase() throws Exception {
    System.out.println("CLEARDB" + url + username + password);
    Statement stmt = null;
    try (Connection conn = DriverManager.getConnection(url, username, password)) {
      if (conn != null) {
        System.out.println("Connected to the database!");
        stmt = conn.createStatement();
        String sql = "DELETE FROM gateway_cache";
        stmt.executeUpdate(sql);
        sql = "DELETE FROM request_log";
        stmt.execute(sql);
      } else {
        System.out.println("Failed to make connection!");
      }
    } finally {
      try {
        if (stmt != null)
          stmt.close();
      } catch (SQLException ignored) {
      }
    }
  }
}
