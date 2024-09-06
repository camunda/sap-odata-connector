package io.camunda.sap_integration;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.sap_integration.model.ErrorCodes;
import io.camunda.sap_integration.model.ODataConnectorRequest;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V2;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V4;
import io.vavr.control.Try;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ErrorCodesTest {
  @Test
  void destination_error() {
    var input =
        new ODataConnectorRequest(
            "willthrow",
            "/not/important",
            "whocares",
            new Get(null, null, null, null, null, null, new V2(null)));

    var context = OutboundConnectorContextBuilder.create().variables(input).build();

    var function = new ODataConnector();

    ConnectorException exception =
        assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
    assertThat(exception.getErrorCode()).isEqualTo(String.valueOf(ErrorCodes.DESTINATION_ERROR));
  }

  @Nested
  class odata {
    @BeforeEach
    // enable static destination resolution independent of the env var
    void mockDestination() {
      DestinationAccessor.setLoader(null);
      var destination =
          DefaultHttpDestination.builder("http://localhost:4004")
              .authenticationType(AuthenticationType.BASIC_AUTHENTICATION)
              .basicCredentials("alice", "password")
              .trustAllCertificates()
              .build();
      DestinationAccessor.prependDestinationLoader((name, options) -> Try.success(destination));
    }

    @AfterEach
    void resetDestination() {
      DestinationAccessor.setLoader(null);
    }

    @Test
    void response_error() {
      var input =
          new ODataConnectorRequest(
              "willResolveToLocalhost4004",
              "/will/cause",
              "fourohfour",
              new Get(null, null, null, null, null, null, new V4(null, null)));

      new JSONObject()
          .put("tpl_Destination", "willResolveToLocalhost4004")
          .put("tpl_HttpMethod", "GET")
          .put("tpl_ODataService", "/will/cause")
          .put("tpl_EntityOrEntitySet", "fourohfour")
          .put("tpl_ODataVersion", "V4");

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();

      ConnectorException exception =
          assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
      assertThat(exception.getErrorCode()).isEqualTo(String.valueOf("404"));
    }

    @Test
    void request_error() {
      // construct an unreachable endpoint
      var destination =
          DefaultHttpDestination.builder(
                  "http://localhost:4005") //> :4004 is where the real mockserver listens
              .build();
      DestinationAccessor.prependDestinationLoader((name, options) -> Try.success(destination));
      var input =
          new ODataConnectorRequest(
              "resolvesToLocalhost4005",
              "/doesnt/matter",
              "entity",
              new Get(null, null, null, null, null, null, new V4(null, null)));

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();

      ConnectorException exception =
          assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
      assertThat(exception.getErrorCode()).isEqualTo(String.valueOf(ErrorCodes.REQUEST_ERROR));
    }
  }
}
