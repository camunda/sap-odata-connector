package io.camunda.sap_integration;

import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.sap_integration.model.ErrorCodes;
import io.vavr.control.Try;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ErrorCodesTest {
  @Test
  void destination_error() {
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

  @Nested
  class odata {
    @BeforeEach
      // enable static destination resolution independent of the env var
    void mockDestination() {
      DestinationAccessor.setLoader(null);
      var destination = DefaultHttpDestination.builder("http://localhost:4004")
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
      var input = new JSONObject()
          .put("tpl_Destination", "willResolveToLocalhost4004")
          .put("tpl_HttpMethod", "GET")
          .put("tpl_ODataService", "/will/cause")
          .put("tpl_EntityOrEntitySet", "fourohfour")
          .put("tpl_ODataVersion", "V4");

      var context = OutboundConnectorContextBuilder.create()
          .variables(input.toString())
          .build();

      var function = new SAPconnector();

      ConnectorException exception = assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
      assertThat(exception.getErrorCode()).isEqualTo(String.valueOf("404"));
    }

    @Test
    void request_error() {
      // construct an unreachable endpoint
      var destination = DefaultHttpDestination.builder("http://localhost:4005") //> :4004 is where the real mockserver listens
          .build();
      DestinationAccessor.prependDestinationLoader((name, options) -> Try.success(destination));

      var input = new JSONObject()
          .put("tpl_Destination", "resolvesToLocalhost4005")
          .put("tpl_HttpMethod", "GET")
          .put("tpl_ODataService", "/doesnt/matter")
          .put("tpl_EntityOrEntitySet", "entity")
          .put("tpl_ODataVersion", "V4");

      var context = OutboundConnectorContextBuilder.create()
          .variables(input.toString())
          .build();

      var function = new SAPconnector();

      ConnectorException exception = assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
      assertThat(exception.getErrorCode()).isEqualTo(String.valueOf(ErrorCodes.REQUEST_ERROR));
    }
  }


}
