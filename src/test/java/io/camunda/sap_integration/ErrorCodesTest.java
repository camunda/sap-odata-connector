package io.camunda.sap_integration;

import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.sap_integration.model.ErrorCodes;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ErrorCodesTest {
  @Test
  void test_destination_error() {
    var input = new JSONObject()
        .put("tpl_Destination", "willthrow")
        .put("tpl_HttpMethod", "GET")
        .put("tpl_ODataService", "/not/important")
        .put("tpl_EntityOrEntitySet", "whocares")
        .put("tpl_ODataVersion", ODataProtocol.V2);

    var context = OutboundConnectorContextBuilder.create()
        .variables(input.toString())
        .build();

    var function = new SAPconnector();

    ConnectorException exception = assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
    assertThat(exception.getErrorCode()).isEqualTo(String.valueOf(ErrorCodes.DESTINATION_ERROR));
  }
}
