package io.camunda.connector.sap.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

public record ODataConnectorResponseWithCount(
    JsonNode result, int statusCode, Optional<Long> countOrInlineCount) {}
