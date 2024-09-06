package io.camunda.sap_integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RFCConnectorTest {
  @Test
  void connect() {
    var function = new RFCConnector();
    var response = function.execute(null);
    assertThat(response).isNotNull();
  }
}
