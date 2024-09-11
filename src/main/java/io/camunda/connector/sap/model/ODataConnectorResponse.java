package io.camunda.connector.sap.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ODataConnectorResponse(JsonNode result, int statusCode) {}
