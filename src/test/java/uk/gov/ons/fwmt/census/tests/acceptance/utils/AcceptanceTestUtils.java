package uk.gov.ons.fwmt.census.tests.acceptance.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public final class AcceptanceTestUtils {

  @Value("${service.mocktm.url}")
  private String mockTmURL;

  private RestTemplate restTemplate = new RestTemplate();

  public void clearQueues() throws URISyntaxException {
    clearQueue("gateway.actions");
    clearQueue("gateway.actions.DLQ");
    clearQueue("gateway.feedback");
    clearQueue("gateway.actions.DLQ");
    clearQueue("rm.feedback");
    clearQueue("rm.feedback.DLQ");
    clearQueue("Action.Field");
    clearQueue("Action.FieldDLQ");
  }

  public void clearQueue(String queueName) throws URISyntaxException {
    URI uri = new URI(mockTmURL + "/queue/?qname=" + queueName);
    restTemplate.delete(uri);
  }

  public void resetMock() throws IOException, TimeoutException {
    URL url = new URL(mockTmURL + "/logger/reset");
    log.info("rest-mock_url:" + url.toString());
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    httpURLConnection.setRequestMethod("GET");
    if (httpURLConnection.getResponseCode() != 200) {
      throw new RuntimeException("Failed : HTTP error code : "
          + httpURLConnection.getResponseCode());
    }
  }

}
