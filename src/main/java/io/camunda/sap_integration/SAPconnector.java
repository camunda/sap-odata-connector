package io.camunda.sap_integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.UpdateStrategy;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.error.ConnectorExceptionBuilder;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.sap_integration.model.UserDefinedRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;


@OutboundConnector(name = "SAPOUTBOUNDCONNECTOR", inputVariables = {
    "tpl_Destination",
    "tpl_HttpMethod",
    "tpl_ODataService",
    "tpl_EntityOrEntitySet",
    "tpl_ODataVersion",
    "tpl_Payload",
    "tpl_filter",
    "tpl_top",
    "tpl_skip",
    "tpl_orderby",
    "tpl_expand",
    "tpl_select",
    "tpl_inlinecount",
    "tpl_count",
    "tpl_search"
}, type = "io.camunda:sap:outbound:1")
@ElementTemplate(id = "io.camunda.connector.SAP.outbound.v1", name = "SAP connector", version = 1, icon = "sap-connector-outbound.svg", documentationRef = "https://docs.camunda.io/xxx", inputDataClass = UserDefinedRequest.class)
public class SAPconnector implements OutboundConnectorFunction {

  private static final Logger LOGGER = LoggerFactory.getLogger(SAPconnector.class);

  @Override
  public Object execute(OutboundConnectorContext context) throws ConnectorException {
    validateInput(context); //> throws
    // final var connectorRequest = context.bindVariables(UserDefinedRequest.class);
    var connectorRequest = context.getJobContext().getVariables();
    return executeConnector(connectorRequest);
  }

  void validateInput(OutboundConnectorContext context) {
    JSONObject json = new JSONObject(context.getJobContext().getVariables());
    if (json.has("tpl_Payload")) {
      ObjectMapper om = ConnectorsObjectMapperSupplier.getCopy();
      try {
        om.readTree(json.get("tpl_Payload").toString());
      } catch (JsonProcessingException e) {
        throw new ConnectorExceptionBuilder()
            .message("invalid JSON payload: " + e.getMessage())
            .errorCode("INVALID_PAYLOAD")
            .build();
      }
    }
  }

  private Object executeConnector(String context) {
    LOGGER.debug("Executing my connector with request {}", context);

    JSONObject json = new JSONObject(context);

    String httpMethod = json.getString("tpl_HttpMethod");

    ODataProtocol oDataVersion = json.getString("tpl_ODataVersion").equalsIgnoreCase("v2") ? ODataProtocol.V2 : ODataProtocol.V4;

    // get ourselves the query parameters from user-space connector design time
    HashMap<String, String> queryParams = new HashMap<>();
    Arrays.asList("tpl_top", "tpl_skip", "tpl_count", "tpl_expand", "tpl_filter", "tpl_search", "tpl_select", "tpl_orderby").forEach(queryParam -> {
      if (json.has(queryParam) && !json.isNull(queryParam)) {
        if (queryParam.equals("tpl_count")) {
          if (oDataVersion.equals(ODataProtocol.V2)) {
            queryParams.put("$inlinecount", "allpages");
          } else {
            queryParams.put("$count", "true");
          }
        } else {
          queryParams.put("$" + queryParam.substring(4) /* eliminate "tpl_" */, json.getString(queryParam));
        }
      }
    });

    ODataRequest OData = new ODataRequest(
        json.getString("tpl_Destination"),
        json.getString("tpl_ODataService"),
        json.getString("tpl_EntityOrEntitySet"),
        queryParams,
        oDataVersion);

    Object result = ODataRequest.defaultResponse;

    result = switch (httpMethod.toLowerCase()) {
      case "get" -> OData.get();
      case "post" -> OData.post(json.get("tpl_Payload").toString());
      case "put" -> handlePutOrPatch(OData, json, UpdateStrategy.REPLACE_WITH_PUT);
      case "patch" -> handlePutOrPatch(OData, json, UpdateStrategy.MODIFY_WITH_PATCH);
      case "delete" -> OData.delete();
      default -> result;
    };

    return result;

  }

  private Object handlePutOrPatch(ODataRequest OData, JSONObject tplAsJson, UpdateStrategy strategy) {
    ODataResourcePath resourcePath = ODataResourcePath.of(tplAsJson.getString("tpl_EntityOrEntitySet"));
    return OData.putOrPatch(strategy, resourcePath, tplAsJson.get("tpl_Payload").toString());
  }
}
