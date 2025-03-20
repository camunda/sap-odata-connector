package io.camunda.connector.sap.odata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationType;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResult;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResultGeneric;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.sap.odata.model.*;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest.ODataVersion;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataServiceErrorException;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestGeneric;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResultMultipartGeneric;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

public class ODataBatchRequestExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ODataBatchRequestExecutor.class);

  public Object executeBatch(ODataConnectorRequest request) {
    Destination destination = buildDestination(request.destination());
    LOGGER.debug("Destination: {}", destination);
    HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);

    ODataProtocol protocol = determineProtocol(ODataConnectorRequestAccessor.oDataVersion(request));

    BatchRequestBuilder builder = new BatchRequestBuilder();
    builder.setODataVersion(protocol);
    builder.setODataService(request.oDataService());
    try {
      var payloadAsString = builder.getMapper().writeValueAsString(request.batchRequestPayload());
      builder.buildSource(payloadAsString).buildRequest();
      ODataRequestResultMultipartGeneric batchResult = builder.getBatch().execute(httpClient);
      return buildBatchResponse(builder, batchResult, protocol);
    } catch (JsonProcessingException e) {
      throw new ConnectorException(
          ErrorCodes.GENERIC_ERROR.name(), buildErrorMsg(e, "OData Batch runtime error: "), e);

    } catch (ODataServiceErrorException e) {
      throw new ConnectorException(
          ErrorCodes.REQUEST_ERROR.name(), buildErrorMsg(e, "OData Batch request error: "), e);
    }
  }

  private Record buildBatchResponse(
      BatchRequestBuilder builder,
      ODataRequestResultMultipartGeneric batchResult,
      ODataProtocol oDataVersion)
      throws JsonProcessingException {
    var originalRequests = builder.getRequests();
    var responses = new ArrayList<>();
    for (ODataRequestGeneric originalRequest : originalRequests) {
      var result = batchResult.getResult(originalRequest);
      var response =
          buildResponse(
              result,
              oDataVersion.getProtocolVersion().equals("V2") ? ODataVersion.V2 : ODataVersion.V4);
      responses.add(response);
    }
    return new ODataConnectorBatchResponse(responses);
  }

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