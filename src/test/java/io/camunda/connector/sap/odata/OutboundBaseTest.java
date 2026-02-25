package io.camunda.connector.sap.odata;

import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;

public class OutboundBaseTest extends BaseTest {

  public static OutboundConnectorContextBuilder getContextBuilder() {
    return OutboundConnectorContextBuilder.create()
        .secret(SecretsConstant.DESTINATION, ActualValue.DESTINATION);
  }
}
