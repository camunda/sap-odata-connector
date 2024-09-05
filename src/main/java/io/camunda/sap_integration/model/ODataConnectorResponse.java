package io.camunda.sap_integration.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ODataConnectorResponse(JsonNode result, int statusCode) {}
