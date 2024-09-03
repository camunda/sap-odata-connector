package io.camunda.sap_integration;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationProperty;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationType;

public class DestinationProvider {
  private DestinationProvider() {}

  public static Destination getDestination(
      String destination, DestinationType destinationType, boolean trustAllCertificates
  ) {
    Destination baseDestination = DefaultDestination
        .builder()
        .name(destination)
        .property(DestinationProperty.TRUST_ALL.getKeyName(), trustAllCertificates)
        .build();
    if (destinationType.equals(DestinationType.HTTP)) {
      return baseDestination.asHttp();
    } else if (destinationType.equals(DestinationType.RFC)) {
      return baseDestination.asRfc();
    } else {
      throw new IllegalArgumentException("Destination type not supported: " + destinationType);
    }
  }
}
