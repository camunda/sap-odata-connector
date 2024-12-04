package io.camunda.connector.sap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.sap.model.ErrorCodes;
import io.camunda.connector.sap.model.ODataConnectorRequest;
import io.camunda.connector.sap.model.ODataConnectorRequest.HttpMethod.Get;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.vavr.control.Try;
import org.junit.jupiter.api.*;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@EnabledIf(
    value =
        "#{environment.getActiveProfiles().length == 0 || {'default','unit'}.contains(environment.getActiveProfiles()[0])}",
    loadContext = true)
public class ErrorCodesTest {
  @Test
  void destination_error() {
    var input =
        new ODataConnectorRequest(
            "willthrow",
            "/not/important",
            "whocares",
            new Get(null, null, null, null, null, null, new Get.ODataVersionGet.V2(false)),
            null);

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
              new Get(null, null, null, null, null, null, new Get.ODataVersionGet.V4(null, null)),
              null);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();

      ConnectorException exception =
          assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
      assertThat(exception.getErrorCode()).isEqualTo("404");
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
              new Get(null, null, null, null, null, null, new Get.ODataVersionGet.V4(null, null)),
              null);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();

      ConnectorException exception =
          assertThrowsExactly(ConnectorException.class, () -> function.execute(context));
      assertThat(exception.getErrorCode()).isEqualTo(String.valueOf(ErrorCodes.REQUEST_ERROR));
    }
  }
}
