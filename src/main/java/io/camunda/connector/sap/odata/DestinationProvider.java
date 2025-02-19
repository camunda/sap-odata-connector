package io.camunda.connector.sap.odata;

import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationType;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.sap.odata.model.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DestinationProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DestinationProvider.class);

  private static final int MAX_RETRIES = 10;
  private static final long RETRY_DELAY_MS = 500;

  private DestinationProvider() {}

  public static Destination getDestination(String destination, DestinationType destinationType) {
    int attempt = 0;
    while (attempt < MAX_RETRIES) {
      try {
        Destination d = DestinationAccessor.getDestination(destination);
        if (destinationType.equals(DestinationType.HTTP)) {
          // TODO for POI deployment: "trustAllCertificates"
          return d.asHttp();
        } else {
          throw new IllegalArgumentException("Only supporting destination of type 'HTTP'!");
        }
      } catch (Exception e) {
        LOGGER.warn("Failed to get destination at try {}, retrying...", attempt);
        attempt++;
        if (attempt >= MAX_RETRIES) {
          LOGGER.error("Failed to get destination after {} retries", MAX_RETRIES);
          throw new ConnectorException(ErrorCodes.DESTINATION_ERROR.name(), e.getMessage(), e);
        }
        try {
          Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new ConnectorException(
              ErrorCodes.DESTINATION_ERROR.name(), "Retry interrupted", ie);
        }
      }
    }
    throw new ConnectorException(
        ErrorCodes.DESTINATION_ERROR.name(),
        "Finally failed to get destination after " + MAX_RETRIES + " retries");
  }
}
