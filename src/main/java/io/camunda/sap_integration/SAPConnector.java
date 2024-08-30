package io.camunda.sap_integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestCreate;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestDelete;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestExecutable;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestRead;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResult;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResultGeneric;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestUpdate;
import com.sap.cloud.sdk.datamodel.odata.client.request.UpdateStrategy;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.error.ConnectorExceptionBuilder;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.sap_integration.model.UserDefinedRequest;
import org.apache.http.client.HttpClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sap.cloud.sdk.datamodel.odata.client.request.ODataUriFactory.*;

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
@ElementTemplate(id = "io.camunda.connector.SAP.outbound.v1",
    name = "SAP connector",
    version = 1,
    icon = "sap-connector-outbound.svg",
    documentationRef = "https://docs.camunda.io/xxx",
    inputDataClass = UserDefinedRequest.class)
public class SAPConnector implements OutboundConnectorFunction {

  private static final Logger LOGGER = LoggerFactory.getLogger(SAPConnector.class);

  @Override
  public Object execute(OutboundConnectorContext context) throws ConnectorException {
    //    validateInput(context); //> throws
    //    // final var connectorRequest = context.bindVariables(UserDefinedRequest.class);
    //    var connectorRequest = context.getJobContext().getVariables();
    //    return executeConnector(connectorRequest);
    SAPConnectorRequest request = context.bindVariables(SAPConnectorRequest.class);
    return executeRequest(request);
  }

  private SAPConnectorResponse executeRequest(SAPConnectorRequest request) {
    Destination destination = buildDestination(request.destination());
    HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);
    ODataRequestExecutable oDataRequest = buildRequest(request);
    ODataRequestResult oDataResponse = oDataRequest.execute(httpClient);
    return buildResponse(oDataResponse);
  }

  private SAPConnectorResponse buildResponse(ODataRequestResult oDataResponse) {
    oDataResponse.getHttpResponse().getEntity().
    return new SAPConnectorResponse(result, statusCode);
  }

  private ODataRequestExecutable buildRequest(SAPConnectorRequest request) {
    ODataProtocol protocol = determineProtocol(request.oDataVersion());
    ODataResourcePath path = ODataResourcePath.of(request.entityOrEntitySet());
    if (request.httpMethod() instanceof Get) {
      String encodedQuery = encodeQuery(createQuery(request.queryParams()));
      return new ODataRequestRead(request.oDataService(), request.entityOrEntitySet(), encodedQuery, protocol);
    } else if (request.httpMethod() instanceof Post post) {
      String serializedEntity = createSerializedEntity(post.payload());
      return new ODataRequestCreate(request.oDataService(), request.entityOrEntitySet(), serializedEntity, protocol);
    } else if (request.httpMethod() instanceof Delete) {
      return new ODataRequestDelete(request.oDataService(), path, null, protocol);
    } else if (request.httpMethod() instanceof Put put) {
      String serializedEntity = createSerializedEntity(put.payload());
      return new ODataRequestUpdate(request.oDataService(),
          path,
          serializedEntity,
          UpdateStrategy.REPLACE_WITH_PUT,
          null,
          protocol
      );
    } else if (request.httpMethod() instanceof Patch patch) {
      String serializedEntity = createSerializedEntity(patch.payload());
      return new ODataRequestUpdate(request.oDataService(),
          path,
          serializedEntity,
          UpdateStrategy.MODIFY_WITH_PATCH,
          null,
          protocol
      );
    } else {
      throw new IllegalStateException("Unsupported HTTP method: " + request.httpMethod());
    }
  }

  private String createSerializedEntity(Map<String, Object> entity) {
    try {
      return ConnectorsObjectMapperSupplier.DEFAULT_MAPPER.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error while serializing payload", e);
    }
  }

  private String createQuery(Map<String, String> queryParams) {
    return queryParams
        .entrySet()
        .stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
        .collect(Collectors.joining("&"));
  }

  private Destination buildDestination(String destination) {
    // TODO: map "trustAllCertificates" to an element template option
    return DestinationAccessor.getDestination(destination);
  }

  private SAPConnectorResponse executeGet(SAPConnectorRequest connectorRequest, ODataProtocol protocol) {
    ODataRequestRead request = new ODataRequestRead(connectorRequest.oDataService(),
        connectorRequest.entityOrEntitySet(),
        "",
        // encodedQuery
        protocol
    );
    connectorRequest
        .queryParams()
        .forEach(request::addQueryParameter);

    ODataRequestResultGeneric _result = request.execute(this.client);
    Map result = mapResponseToProtocol(_result);

    LOGGER.debug("//> response {}", result);
    return result;
  }

  private ODataProtocol determineProtocol(ODataVersion oDataVersion) {
    if (oDataVersion.equals(ODataVersion.v2)) {
      return ODataProtocol.V2;
    } else if (oDataVersion.equals(ODataVersion.v4)) {
      return ODataProtocol.V4;
    }
    throw new IllegalStateException("Unknown protocol " + oDataVersion);
  }

  void validateInput(OutboundConnectorContext context) {
    JSONObject json = new JSONObject(context
        .getJobContext()
        .getVariables());
    if (json.has("tpl_Payload")) {
      ObjectMapper om = ConnectorsObjectMapperSupplier.getCopy();
      try {
        om.readTree(json
            .get("tpl_Payload")
            .toString());
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

    ODataProtocol oDataVersion = json
        .getString("tpl_ODataVersion")
        .equalsIgnoreCase("v2") ? ODataProtocol.V2 : ODataProtocol.V4;

    // get ourselves the query parameters from user-space connector design time
    HashMap<String, String> queryParams = new HashMap<>();
    Arrays
        .asList("tpl_top",
            "tpl_skip",
            "tpl_count",
            "tpl_expand",
            "tpl_filter",
            "tpl_search",
            "tpl_select",
            "tpl_orderby"
        )
        .forEach(queryParam -> {
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
    return OData.putOrPatch(strategy,
        resourcePath,
        tplAsJson
            .get("tpl_Payload")
            .toString()
    );
  }
}
