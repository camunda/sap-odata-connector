package io.camunda.sap_integration;

import static com.sap.cloud.sdk.datamodel.odata.client.request.ODataUriFactory.*;

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
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestCreate;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestDelete;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestExecutable;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestRead;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResult;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestUpdate;
import com.sap.cloud.sdk.datamodel.odata.client.request.UpdateStrategy;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.sap_integration.model.ErrorCodes;
import io.camunda.sap_integration.model.SAPConnectorRequest;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Delete;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Get;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Patch;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Post;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Put;
import io.camunda.sap_integration.model.SAPConnectorRequest.ODataVersion;
import io.camunda.sap_integration.model.SAPConnectorResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OutboundConnector(
    name = "SAPOUTBOUNDCONNECTOR",
    inputVariables = {},
    type = "io.camunda:sap:outbound:1")
@ElementTemplate(
    id = "io.camunda.connector.SAP.outbound.v1",
    name = "SAP connector",
    version = 1,
    icon = "sap-connector-outbound.svg",
    documentationRef = "https://docs.camunda.io/xxx",
    inputDataClass = SAPConnectorRequest.class)
public class SAPConnector implements OutboundConnectorFunction {

  private static final Logger LOGGER = LoggerFactory.getLogger(SAPConnector.class);

  @Override
  public Object execute(OutboundConnectorContext context) {
    SAPConnectorRequest request = context.bindVariables(SAPConnectorRequest.class);
    return executeRequest(request);
  }

  private SAPConnectorResponse executeRequest(SAPConnectorRequest request) {
    Destination destination = buildDestination(request.destination());
    HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);
    ODataRequestExecutable oDataRequest = buildRequest(request);
    try {
      ODataRequestResult oDataResponse = oDataRequest.execute(httpClient);
      return buildResponse(oDataResponse, SAPConnectorRequestAccessor.oDataVersion(request));
    } catch (ODataRequestException e) {
      throw new ConnectorException(ErrorCodes.REQUEST_ERROR.name(), e.getMessage(), e);
    } catch (ODataResponseException e) {
      throw new ConnectorException(String.valueOf(e.getHttpCode()), e.getMessage(), e);
    }
  }

  private SAPConnectorResponse buildResponse(
      ODataRequestResult oDataResponse, ODataVersion oDataVersion) {
    JsonNode responseBody = readResponseBody(oDataResponse);
    int statusCode = oDataResponse.getHttpResponse().getStatusLine().getStatusCode();
    if (responseBody.isNull()) {
      return new SAPConnectorResponse(responseBody, statusCode);
    }
    if (oDataVersion.equals(ODataVersion.V2)) {
      return buildV2Response(responseBody, statusCode);
    } else if (oDataVersion.equals(ODataVersion.V4)) {
      return buildV4Response(responseBody, statusCode);
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

  private SAPConnectorResponse buildV4Response(JsonNode responseBody, int statusCode) {
    if (responseBody.has("value")) {
      return new SAPConnectorResponse(responseBody.get("value"), statusCode);
    }
    return new SAPConnectorResponse(responseBody, statusCode);
  }

  private SAPConnectorResponse buildV2Response(JsonNode responseBody, int statusCode) {
    JsonNode d = responseBody.get("d");
    if (d.has("results")) {
      return new SAPConnectorResponse(d.get("results"), statusCode);
    }
    return new SAPConnectorResponse(d, statusCode);
  }

  private ODataRequestExecutable buildRequest(SAPConnectorRequest request) {
    ODataProtocol protocol = determineProtocol(SAPConnectorRequestAccessor.oDataVersion(request));
    ODataResourcePath path = ODataResourcePath.of(request.entityOrEntitySet());
    switch (request.httpMethod()) {
      case Get get -> {
        String encodedQuery =
            encodeQuery(createQuery(SAPConnectorRequestAccessor.queryParams(get)));
        return new ODataRequestRead(
            request.oDataService(), request.entityOrEntitySet(), encodedQuery, protocol);
      }
      case Post post -> {
        String serializedEntity = createSerializedEntity(post.payloadPost());
        return new ODataRequestCreate(
            request.oDataService(), request.entityOrEntitySet(), serializedEntity, protocol);
      }
      case Delete ignored -> {
        return new ODataRequestDelete(request.oDataService(), path, null, protocol);
      }
      case Put put -> {
        String serializedEntity = createSerializedEntity(put.payloadPut());
        return new ODataRequestUpdate(
            request.oDataService(),
            path,
            serializedEntity,
            UpdateStrategy.REPLACE_WITH_PUT,
            null,
            protocol);
      }
      case Patch patch -> {
        String serializedEntity = createSerializedEntity(patch.payloadPatch());
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

  private String createQuery(Map<String, String> queryParams) {
    return queryParams.entrySet().stream()
        .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
        .collect(Collectors.joining("&"));
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
