package io.camunda.connector.sap.odata.model;

import java.util.ArrayList;

public record ODataConnectorBatchResponse(ArrayList<Object> batchResponses) {}
