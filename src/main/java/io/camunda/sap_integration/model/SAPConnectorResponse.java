package io.camunda.sap_integration.model;

public record SAPConnectorResponse(
  Object result,
  int statusCode
) {}
