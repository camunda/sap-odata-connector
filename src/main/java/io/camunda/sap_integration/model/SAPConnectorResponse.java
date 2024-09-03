package io.camunda.sap_integration.model;

import com.fasterxml.jackson.databind.JsonNode;

public record SAPConnectorResponse(
  JsonNode result,
  int statusCode
) {}
