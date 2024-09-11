package io.camunda.connector.sap.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ODataConnectorResponseWithCount(
    JsonNode result, int statusCode, int countOrInlineCount) {}
