package io.camunda.connector.sap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataRequestException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.*;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.connector.sap.model.*;
import io.camunda.connector.sap.model.ODataConnectorRequest.HttpMethod.*;
import io.camunda.connector.sap.model.ODataConnectorRequest.ODataVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OutboundConnector(
    name = "SAPOUTBOUNDCONNECTOR",
    inputVariables = {},
    type = "io.camunda:sap:odata:outbound:")
@ElementTemplate(
    id = "io.camunda.connector.sap.odata.outbound.v1",
    name = "SAP OData Connector",
    version = 1,
    //    icon = "sap-odata-connector-outbound.svg",
    documentationRef = "https://docs.camunda.io/xxx",
    inputDataClass = ODataConnectorRequest.class,
    propertyGroups = {
      @ElementTemplate.PropertyGroup(id = "sap", label = "SAP"),
      @ElementTemplate.PropertyGroup(id = "advanced", label = "Advanced")
    })
public class ODataConnector implements OutboundConnectorFunction {

  private static final Logger LOGGER = LoggerFactory.getLogger(ODataConnector.class);

  @Override
  public Object execute(OutboundConnectorContext context) {
    ODataConnectorRequest request = context.bindVariables(ODataConnectorRequest.class);
    return executeRequest(request);
  }

  private Record executeRequest(ODataConnectorRequest request) {
    Destination destination = buildDestination(request.destination());
    HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);
    ODataRequestExecutable oDataRequest = buildRequest(request);
    try {
      ODataRequestResult oDataResponse = oDataRequest.execute(httpClient);
      return buildResponse(oDataResponse, ODataConnectorRequestAccessor.oDataVersion(request));
    } catch (ODataRequestException e) {
      throw new ConnectorException(ErrorCodes.REQUEST_ERROR.name(), e.getMessage(), e);
    } catch (ODataResponseException e) {
      throw new ConnectorException(String.valueOf(e.getHttpCode()), e.getMessage(), e);
    }
  }

  private Record buildResponse(ODataRequestResult oDataResponse, ODataVersion oDataVersion) {
    JsonNode responseBody = readResponseBody(oDataResponse);
    int statusCode = oDataResponse.getHttpResponse().getStatusLine().getStatusCode();

    Optional<Long> countOrInlineCount = Optional.empty();
    try {
      long count = ((ODataRequestResultGeneric) oDataResponse).getInlineCount();
      if (count != 0) {
        countOrInlineCount = Optional.of(count);
      }
    } catch (Exception e) {
      LOGGER.debug("no count or inlinecount property found in response");
    }

    if (responseBody.isNull()) {
      return new ODataConnectorResponse(responseBody, statusCode);
    }
    if (oDataVersion.equals(ODataVersion.V2)) {
      return buildV2Response(responseBody, statusCode, countOrInlineCount);
    } else if (oDataVersion.equals(ODataVersion.V4)) {
      return buildV4Response(responseBody, statusCode, countOrInlineCount);
    } else {
      throw new IllegalArgumentException("Unsupported version: " + oDataVersion);
    }
  }

  private JsonNode readResponseBody(ODataRequestResult oDataResponse) {
    if (oDataResponse.getHttpResponse().getEntity() == null) {
      return JsonNodeFactory.instance.nullNode();
    }
    try (InputStream in = oDataResponse.getHttpResponse().getEntity().getContent()) {
      return ConnectorsObjectMapperSupplier.DEFAULT_MAPPER.readTree(in);
    } catch (IOException e) {
      throw new RuntimeException("Error while reading http response", e);
    }
  }

  private Record buildV4Response(
      JsonNode responseBody, int statusCode, Optional<Long> countOrInlineCount) {
    JsonNode value = responseBody.has("value") ? responseBody.get("value") : responseBody;
    return countOrInlineCount.isPresent()
        ? new ODataConnectorResponseWithCount(value, statusCode, countOrInlineCount)
        : new ODataConnectorResponse(value, statusCode);
  }

  private Record buildV2Response(
      JsonNode responseBody, int statusCode, Optional<Long> countOrInlineCount) {
    JsonNode d = responseBody.get("d");
    JsonNode results = d.has("results") ? d.get("results") : d;
    return countOrInlineCount.isPresent()
        ? new ODataConnectorResponseWithCount(results, statusCode, countOrInlineCount)
        : new ODataConnectorResponse(results, statusCode);
  }

  private ODataRequestExecutable buildRequest(ODataConnectorRequest request) {
    ODataProtocol protocol = determineProtocol(ODataConnectorRequestAccessor.oDataVersion(request));
    ODataResourcePath path = ODataResourcePath.of(request.entityOrEntitySet());
    switch (request.httpMethod()) {
      case Get get -> {
        ODataRequestRead read =
            new ODataRequestRead(request.oDataService(), request.entityOrEntitySet(), "", protocol);
        ODataConnectorRequestAccessor.queryParams(get).forEach(read::addQueryParameter);
        return read;
      }
      case Post post -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new ODataRequestCreate(
            request.oDataService(), request.entityOrEntitySet(), serializedEntity, protocol);
      }
      case Delete ignored -> {
        return new ODataRequestDelete(request.oDataService(), path, null, protocol);
      }
      case Put put -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new ODataRequestUpdate(
            request.oDataService(),
            path,
            serializedEntity,
            UpdateStrategy.REPLACE_WITH_PUT,
            null,
            protocol);
      }
      case Patch patch -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new ODataRequestUpdate(
            request.oDataService(),
            path,
            serializedEntity,
            UpdateStrategy.MODIFY_WITH_PATCH,
            null,
            protocol);
      }
    }
  }

  private String createSerializedEntity(Map<String, Object> entity) {
    try {
      return ConnectorsObjectMapperSupplier.DEFAULT_MAPPER.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error while serializing payload", e);
    }
  }

  private Destination buildDestination(String destination) {
    return DestinationProvider.getDestination(destination, DestinationType.HTTP);
  }

  private ODataProtocol determineProtocol(ODataVersion oDataVersion) {
    if (oDataVersion.equals(ODataVersion.V2)) {
      return ODataProtocol.V2;
    } else if (oDataVersion.equals(ODataVersion.V4)) {
      return ODataProtocol.V4;
    }
    throw new IllegalStateException("Unknown protocol " + oDataVersion);
  }
}
