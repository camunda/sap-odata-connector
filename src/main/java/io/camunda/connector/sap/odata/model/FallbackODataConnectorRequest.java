package io.camunda.connector.sap.odata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * Fallback request model for parsing variables from older element templates (v1) that don't include
 * the {@code requestDetails.requestType} discriminator property.
 *
 * <p>The v1 element template sent variables in a flat structure — {@code destination}, {@code
 * oDataService}, {@code entityOrEntitySet}, {@code httpMethod.*}, {@code payload} — identical to
 * the pre-batch {@code ODataConnectorRequest} shape. The {@link HttpMethod} sealed interface and
 * its Jackson {@code @JsonSubTypes} are structurally unchanged, so this record reuses them directly
 * without any manual conversion.
 *
 * @see ODataConnectorRequest
 * @see ODataRequestDetails.SimpleRequest
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FallbackODataConnectorRequest(
    String destination,
    String oDataService,
    String entityOrEntitySet,
    @Valid HttpMethod httpMethod,
    Map<String, Object> payload) {

  /**
   * Converts this v1 flat representation into a proper {@link ODataConnectorRequest} with a {@link
   * ODataRequestDetails.SimpleRequest}. The v1 element template only supported simple requests (no
   * batch), so the conversion always produces a {@code SimpleRequest}.
   *
   * @return fully typed {@link ODataConnectorRequest}
   */
  public ODataConnectorRequest toODataConnectorRequest() {
    return new ODataConnectorRequest(
        destination,
        oDataService,
        new ODataRequestDetails.SimpleRequest(entityOrEntitySet, httpMethod, payload));
  }
}
