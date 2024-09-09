package io.camunda.connector.sap.common;

public enum ErrorCodes {
  INVALID_PAYLOAD, //> OData v2 + v4 write operations input validation
  REQUEST_ERROR, //> OData v2 + v4 read operations
  GENERIC_ERROR, //> top level error
  DESTINATION_ERROR //> Destination error
}
