package io.camunda.sap_integration;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RFCConnectorTest {
  @Test
  void connect() {
    var function = new RFCConnector();
    var response = function.execute(null);
    assertThat(response).isNotNull();
  }
}
