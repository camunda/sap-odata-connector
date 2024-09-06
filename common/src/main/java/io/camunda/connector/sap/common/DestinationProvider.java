package io.camunda.connector.sap.common;

import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationType;
import io.camunda.connector.api.error.ConnectorException;

public class DestinationProvider {
  private DestinationProvider() {}

  public static Destination getDestination(String destination, DestinationType destinationType) {
    try {
      Destination d = DestinationAccessor.getDestination(destination);
      if (destinationType.equals(DestinationType.HTTP)) {
        // TODO I cannot get a destination and change the property "trustAllCertificates"
        return d.asHttp();
      } else if (destinationType.equals(DestinationType.RFC)) {
        // TODO does RFC even have a parameter to trust certificates or not?
        return d.asRfc();
      } else {
        throw new IllegalArgumentException("Destination type not supported: " + destinationType);
      }
    } catch (Exception e) {
      throw new ConnectorException(ErrorCodes.DESTINATION_ERROR.name(), e.getMessage(), e);
    }
  }
}
