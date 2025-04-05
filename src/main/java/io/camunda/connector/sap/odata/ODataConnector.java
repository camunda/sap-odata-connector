package io.camunda.connector.sap.odata;

import static io.camunda.connector.sap.odata.ODataConnector.*;
import static io.camunda.connector.sap.odata.model.ODataRequestDetails.BatchRequest;

import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest;

@OutboundConnector(
    name = NAME,
    inputVariables = {},
    type = TYPE)
@ElementTemplate(
    id = NAME,
    name = "SAP OData Connector",
    inputDataClass = ODataConnectorRequest.class,
    version = VERSION,
    description = "This connector allows you to interact with an SAP System via OData v2 + v4",
    icon = "sap-odata-connector-outbound.svg",
    documentationRef = "https://docs.camunda.io/docs/components/camunda-integrations/sap",
    propertyGroups = {
      @ElementTemplate.PropertyGroup(id = "sap", label = "SAP"),
      @ElementTemplate.PropertyGroup(id = "batch", label = "Batch Request"),
      @ElementTemplate.PropertyGroup(id = "advanced", label = "Advanced")
    })
public class ODataConnector implements OutboundConnectorFunction {
  public static final String NAME = "SAP_ODATA_CONNECTOR";
  public static final int VERSION = 2;
  // the format "io.camunda:<type>:<version>" is important as this
  // is in line w/ "zeebe-analytics", exporting usage of the connector task to mixpanel (for SaaS)
  public static final String TYPE = "io.camunda:sap-odata" + ":" + VERSION;

  @Override
  public Object execute(OutboundConnectorContext context) {
    ODataConnectorRequest request = context.bindVariables(ODataConnectorRequest.class);
    // check that request.requestDetails() is of type BatchRequest
    if (request.requestDetails() instanceof BatchRequest) {
      ODataBatchRequestExecutor batchExecutor = new ODataBatchRequestExecutor();
      return batchExecutor.executeBatch(request);
    } else {
      ODataRequestExecutor requestExecutor = new ODataRequestExecutor();
      return requestExecutor.executeRequest(request);
    }
  }
}
