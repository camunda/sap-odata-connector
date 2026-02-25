package io.camunda.connector.sap.odata;

import static io.camunda.connector.sap.odata.OutboundBaseTest.getContextBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.sap.odata.model.FallbackODataConnectorRequest;
import io.camunda.connector.sap.odata.model.HttpMethod;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest;
import io.camunda.connector.sap.odata.model.ODataRequestDetails;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Validates that variables from an older element template (v1) — which lack the {@code
 * requestDetails.requestType} discriminator — can be gracefully parsed via {@link
 * FallbackODataConnectorRequest} and converted to a proper {@link ODataConnectorRequest}.
 */
public class HotfixBindingTest extends BaseTest {

  @ParameterizedTest
  @MethodSource("hotfixTestCases")
  void bindingToODataConnectorRequest_losesRequestDetails_forOldETVariables(String input) {
    // Given: JSON input shaped like v1 element template (no requestType discriminator)
    OutboundConnectorContext context = getContextBuilder().variables(input).build();

    // When: binding to ODataConnectorRequest — it doesn't throw, but requestDetails is lost
    var request = context.bindVariables(ODataConnectorRequest.class);

    // Then: top-level fields that don't require a discriminator are still populated
    assertThat(request.destination()).isNotBlank();
    assertThat(request.oDataService()).isNotBlank();

    // Then: requestDetails is null because the v1 ET doesn't provide requestType discriminator
    // — this is the root cause of the customer's runtime failure
    assertThat(request.requestDetails())
        .as(
            "v1 ET variables lack 'requestDetails.requestType', so requestDetails cannot be deserialized")
        .isNull();
  }

  @ParameterizedTest
  @MethodSource("hotfixTestCases")
  void bindingToFallbackRequest_shouldSucceed_forOldETVariables(String input) {
    // Given: JSON input shaped like v1 element template (flat, no requestDetails wrapper)
    OutboundConnectorContext context = getContextBuilder().variables(input).build();

    // When: binding to FallbackODataConnectorRequest — reuses HttpMethod sealed interface directly
    var request = context.bindVariables(FallbackODataConnectorRequest.class);

    // Then: core fields are present
    assertThat(request.destination()).isNotBlank();
    assertThat(request.oDataService()).isNotBlank();
    assertThat(request.entityOrEntitySet()).isNotBlank();

    // Then: httpMethod was deserialized as the correct typed instance via Jackson polymorphism
    assertThat(request.httpMethod()).isNotNull();
    assertThat(request.httpMethod()).isInstanceOf(HttpMethod.class);
  }

  @ParameterizedTest
  @MethodSource("hotfixTestCases")
  void fallbackRequest_convertsToODataConnectorRequest(String input) {
    // Given: fallback-parsed request from v1 ET variables
    OutboundConnectorContext context = getContextBuilder().variables(input).build();
    var fallback = context.bindVariables(FallbackODataConnectorRequest.class);

    // When: converting to a proper ODataConnectorRequest
    ODataConnectorRequest converted = fallback.toODataConnectorRequest();

    // Then: top-level fields are preserved
    assertThat(converted.destination()).isEqualTo(fallback.destination());
    assertThat(converted.oDataService()).isEqualTo(fallback.oDataService());

    // Then: requestDetails is a SimpleRequest (v1 ET only supported simple requests)
    assertThat(converted.requestDetails()).isInstanceOf(ODataRequestDetails.SimpleRequest.class);
    var simple = (ODataRequestDetails.SimpleRequest) converted.requestDetails();
    assertThat(simple.entityOrEntitySet()).isEqualTo(fallback.entityOrEntitySet());
    assertThat(simple.payload()).isEqualTo(fallback.payload());

    // Then: httpMethod is the same typed instance — no conversion needed
    assertThat(simple.httpMethod()).isSameAs(fallback.httpMethod());
  }
}
