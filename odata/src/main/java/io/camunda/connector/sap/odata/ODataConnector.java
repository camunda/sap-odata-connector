package io.camunda.connector.sap.odata;

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
import io.camunda.connector.sap.common.DestinationProvider;
import io.camunda.connector.sap.common.ErrorCodes;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.HttpMethod.Delete;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.HttpMethod.Get;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.HttpMethod.Patch;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.HttpMethod.Post;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.HttpMethod.Put;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.ODataVersion;
import io.camunda.connector.sap.odata.model.ODataConnectorRequestAccessor;
import io.camunda.connector.sap.odata.model.ODataConnectorResponse;
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
    type = "io.camunda:odata:outbound:")
@ElementTemplate(
    id = "io.camunda.connector.OData.outbound.v1",
    name = "SAP oData Connector",
    version = 1,
    icon = "sap-connector-outbound.svg",
    documentationRef = "https://docs.camunda.io/xxx",
    inputDataClass = ODataConnectorRequest.class)
public class ODataConnector implements OutboundConnectorFunction {

  public ODataConnector(ODataConnectorConfiguration configuration){
    // do nothing atm
  }

  public ODataConnector(){
    // by default, use static instance
    this(ODataConnectorConfiguration.getInstance());
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(ODataConnector.class);

  @Override
  public Object execute(OutboundConnectorContext context) {
    ODataConnectorRequest request = context.bindVariables(ODataConnectorRequest.class);
    return executeRequest(request);
  }

  private ODataConnectorResponse executeRequest(ODataConnectorRequest request) {
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

  private ODataConnectorResponse buildResponse(
      ODataRequestResult oDataResponse, ODataVersion oDataVersion) {
    JsonNode responseBody = readResponseBody(oDataResponse);
    int statusCode = oDataResponse.getHttpResponse().getStatusLine().getStatusCode();
    if (responseBody.isNull()) {
      return new ODataConnectorResponse(responseBody, statusCode);
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

  private ODataConnectorResponse buildV4Response(JsonNode responseBody, int statusCode) {
    if (responseBody.has("value")) {
      return new ODataConnectorResponse(responseBody.get("value"), statusCode);
    }
    return new ODataConnectorResponse(responseBody, statusCode);
  }

  private ODataConnectorResponse buildV2Response(JsonNode responseBody, int statusCode) {
    JsonNode d = responseBody.get("d");
    if (d.has("results")) {
      return new ODataConnectorResponse(d.get("results"), statusCode);
    }
    return new ODataConnectorResponse(d, statusCode);
  }

  private ODataRequestExecutable buildRequest(ODataConnectorRequest request) {
    ODataProtocol protocol = determineProtocol(ODataConnectorRequestAccessor.oDataVersion(request));
    ODataResourcePath path = ODataResourcePath.of(request.entityOrEntitySet());
    switch (request.httpMethod()) {
      case Get get -> {
        String encodedQuery =
            encodeQuery(createQuery(ODataConnectorRequestAccessor.queryParams(get)));
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
