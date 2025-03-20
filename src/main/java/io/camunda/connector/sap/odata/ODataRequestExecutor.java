package io.camunda.connector.sap.odata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataRequestException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.*;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.sap.odata.helper.*;
import io.camunda.connector.sap.odata.model.ErrorCodes;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest;
import io.camunda.connector.sap.odata.model.ODataConnectorRequestAccessor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ODataRequestExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ODataRequestExecutor.class);

  public Object executeRequest(ODataConnectorRequest request) {
    Destination destination = CommonExecutor.buildDestination(request.destination());
    LOGGER.debug("Destination: {}", destination);
    HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);

    ODataRequestExecutable oDataRequest = buildRequest(request);

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
      return CommonExecutor.buildResponse(
          oDataResponse, ODataConnectorRequestAccessor.oDataVersion(request));
    } catch (ODataRequestException e) {
      throw new ConnectorException(
          ErrorCodes.REQUEST_ERROR.name(),
          CommonExecutor.buildErrorMsg(e, "OData request error: "),
          e);
    } catch (ODataResponseException e) {
      throw new ConnectorException(
          String.valueOf(e.getHttpCode()),
          CommonExecutor.buildErrorMsg(e, "OData request error: "),
          e);
    } catch (RuntimeException e) {
      throw new ConnectorException(
          ErrorCodes.GENERIC_ERROR.name(),
          CommonExecutor.buildErrorMsg(e, "OData runtime error: "),
          e);
    }
  }

  public ODataRequestExecutable buildRequest(ODataConnectorRequest request) {
    ODataProtocol protocol =
        CommonExecutor.determineProtocol(ODataConnectorRequestAccessor.oDataVersion(request));
    ODataResourcePath path = ODataResourcePath.of(request.entityOrEntitySet());
    switch (request.httpMethod()) {
      case ODataConnectorRequest.HttpMethod.Get get -> {
        ODataRequestRead read =
            new CustomODataRequestRead(
                request.oDataService(), request.entityOrEntitySet(), "", protocol);
        ODataConnectorRequestAccessor.queryParams(get).forEach(read::addQueryParameter);
        return read;
      }
      case ODataConnectorRequest.HttpMethod.Post ignore -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new CustomODataRequestCreate(
            request.oDataService(), path, serializedEntity, protocol);
      }
      case ODataConnectorRequest.HttpMethod.Delete ignored -> {
        return new CustomODataRequestDelete(request.oDataService(), path, null, protocol);
      }
      case ODataConnectorRequest.HttpMethod.Put ignore -> {
        String serializedEntity = createSerializedEntity(request.payload());
        return new CustomODataRequestUpdate(
            request.oDataService(),
            path,
            serializedEntity,
            UpdateStrategy.REPLACE_WITH_PUT,
            null,
            protocol);
      }
      case ODataConnectorRequest.HttpMethod.Patch ignore -> {
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

  public static String createSerializedEntity(Map<String, Object> entity) {
    try {
      return ConnectorsObjectMapperSupplier.DEFAULT_MAPPER.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error while serializing payload", e);
    }
  }
}
