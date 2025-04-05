package io.camunda.connector.sap.odata;

import static io.camunda.connector.sap.odata.ODataConnector.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataRequestException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataServiceErrorException;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.*;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.connector.sap.odata.helper.CustomODataRequestCreate;
import io.camunda.connector.sap.odata.helper.CustomODataRequestDelete;
import io.camunda.connector.sap.odata.helper.CustomODataRequestRead;
import io.camunda.connector.sap.odata.helper.CustomODataRequestUpdate;
import io.camunda.connector.sap.odata.model.*;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.HttpMethod.*;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.ODataVersion;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      @ElementTemplate.PropertyGroup(id = "advanced", label = "Advanced")
    })
public class ODataConnector implements OutboundConnectorFunction {
  public static final String NAME = "SAP_ODATA_CONNECTOR";
  public static final int VERSION = 1;
  public static final String TYPE = "io.camunda:sap-odata" + ":" + VERSION;
  private static final Logger LOGGER = LoggerFactory.getLogger(ODataConnector.class);

  @Getter private ODataRequestExecutable oDataRequest;

  @Override
  public Object execute(OutboundConnectorContext context) {
    ODataConnectorRequest request = context.bindVariables(ODataConnectorRequest.class);
    return executeRequest(request);
  }

  private Record executeRequest(ODataConnectorRequest request) {
    Destination destination = buildDestination(request.destination());
    LOGGER.debug("Destination: {}", destination);
    HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);

    this.oDataRequest = buildRequest(request);

    LOGGER.debug(
        "OData request: {}",
        ((ODataRequestGeneric) oDataRequest).getProtocol()
            + " - "
            + ((ODataRequestGeneric) oDataRequest).getRelativeUri()
            + " - "
            + ((ODataRequestGeneric) oDataRequest).getRequestQuery());
    try {
      LOGGER.debug(
          "OData request start at: "
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")));
      ODataRequestResult oDataResponse = oDataRequest.execute(httpClient);
      LOGGER.debug(
          "OData request finished at: "
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")));
      return buildResponse(oDataResponse, ODataConnectorRequestAccessor.oDataVersion(request));
    } catch (ODataRequestException e) {
      throw new ConnectorException(
          ErrorCodes.REQUEST_ERROR.name(), buildErrorMsg(e, "OData request error: "), e);
    } catch (ODataResponseException e) {
      throw new ConnectorException(
          String.valueOf(e.getHttpCode()), buildErrorMsg(e, "OData request error: "), e);
    } catch (RuntimeException e) {
      throw new ConnectorException(
          ErrorCodes.GENERIC_ERROR.name(), buildErrorMsg(e, "OData runtime error: "), e);
    }
  }

  /**
   * Build the response object based on the OData version.
   *
   * @param oDataResponse what execute(httpClient) returned
   * @param oDataVersion enum: V2, V4
   * @return a ODataConnectorResponse or ODataConnectorResponseWithCount object
   */
  private Record buildResponse(ODataRequestResult oDataResponse, ODataVersion oDataVersion) {
    JsonNode responseBody = readResponseBody(oDataResponse);
    int statusCode = oDataResponse.getHttpResponse().getStatusLine().getStatusCode();

    LOGGER.debug("OData response status code: {}", statusCode);
    LOGGER.debug("OData response headers: {}", oDataResponse.getAllHeaderValues());

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
        ? new ODataConnectorResponseWithCount(
            value, statusCode, countOrInlineCount.get().intValue())
        : new ODataConnectorResponse(value, statusCode);
  }

  private Record buildV2Response(
      JsonNode responseBody, int statusCode, Optional<Long> countOrInlineCount) {
    JsonNode d = responseBody.get("d");
    JsonNode results = d.has("results") ? d.get("results") : d;
    return countOrInlineCount.isPresent()
        ? new ODataConnectorResponseWithCount(
            results, statusCode, countOrInlineCount.get().intValue())
        : new ODataConnectorResponse(results, statusCode);
  }

  public ODataRequestExecutable buildRequest(ODataConnectorRequest request) {
    ODataProtocol protocol = determineProtocol(ODataConnectorRequestAccessor.oDataVersion(request));
    ODataResourcePath path = ODataResourcePath.of(request.entityOrEntitySet());
    switch (request.httpMethod()) {
      case Get get -> {
        ODataRequestRead read =
            new CustomODataRequestRead(
                request.oDataService(), request.entityOrEntitySet(), "", protocol);
        ODataConnectorRequestAccessor.queryParams(get).forEach(read::addQueryParameter);
        return read;
      }
      case Post ignore -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new CustomODataRequestCreate(
            request.oDataService(), path, serializedEntity, protocol);
      }
      case Delete ignored -> {
        return new CustomODataRequestDelete(request.oDataService(), path, null, protocol);
      }
      case Put ignore -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new CustomODataRequestUpdate(
            request.oDataService(),
            path,
            serializedEntity,
            UpdateStrategy.REPLACE_WITH_PUT,
            null,
            protocol);
      }
      case Patch ignore -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new CustomODataRequestUpdate(
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

  private static String buildErrorMsg(Exception e, String prefix) {
    String msg = !prefix.isBlank() ? prefix + e.getMessage() : prefix;
    msg += e.getCause() != null ? " caused by: " + e.getCause().getMessage() : "";
    if (e instanceof ODataServiceErrorException oerr) {
      msg += " caused by: " + oerr.getOdataError();
    }
    return msg;
  }
}
