package io.camunda.connector.sap.odata.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ODataConnectorResponse(JsonNode result, int statusCode) {}
