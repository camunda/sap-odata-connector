package io.camunda.connector.sap.odata.model;

public enum ErrorCodes {
  BATCH_SERVICE_ERROR, //> generic error with a $batch
  INVALID_PAYLOAD, //> OData v2 + v4 write operations input validation
  REQUEST_ERROR, //> OData v2 + v4 read operations
  GENERIC_ERROR, //> top level error
  DESTINATION_ERROR //> Destination error
}
