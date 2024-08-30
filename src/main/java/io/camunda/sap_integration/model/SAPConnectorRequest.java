package io.camunda.sap_integration.model;

import java.util.Map;

public record SAPConnectorRequest(
    String destination,
    String oDataService,
    String entityOrEntitySet,
    Map<String, String> queryParams,
    ODataVersion oDataVersion,
    HttpMethod httpMethod
) {
  public enum ODataVersion {
    v2, v4
  }

  public sealed interface HttpMethod {
    public record Get() implements HttpMethod {}

    public record Post(Map<String, Object> payload) implements HttpMethod {}

    public record Put(Map<String, Object> payload) implements HttpMethod {}

    public record Delete() implements HttpMethod {}

    public record Patch(Map<String, Object> payload) implements HttpMethod {}
  }
}
