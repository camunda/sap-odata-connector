package io.camunda.connector.sap.odata.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationType;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataServiceErrorException;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestGeneric;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResult;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResultGeneric;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import io.camunda.connector.sap.odata.DestinationProvider;
import io.camunda.connector.sap.odata.model.ErrorCodes;
import io.camunda.connector.sap.odata.model.HttpMethod;
import io.camunda.connector.sap.odata.model.ODataConnectorResponse;
import io.camunda.connector.sap.odata.model.ODataConnectorResponseWithCount;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(CommonExecutor.class);

  public static ODataProtocol determineProtocol(HttpMethod.ODataVersion oDataVersion) {
    if (oDataVersion.equals(HttpMethod.ODataVersion.V2)) {
      return ODataProtocol.V2;
    } else if (oDataVersion.equals(HttpMethod.ODataVersion.V4)) {
      return ODataProtocol.V4;
    }
    throw new IllegalStateException("Unknown protocol " + oDataVersion);
  }

  public static Destination buildDestination(String destination) {
    return DestinationProvider.getDestination(destination, DestinationType.HTTP);
  }

  public static JsonNode readResponseBody(ODataRequestResult oDataResponse) {
    if (oDataResponse.getHttpResponse().getEntity() == null) {
      return JsonNodeFactory.instance.nullNode();
    }
    try (InputStream in = oDataResponse.getHttpResponse().getEntity().getContent()) {
      return ConnectorsObjectMapperSupplier.getCopy().readTree(in);
    } catch (IOException e) {
      throw new RuntimeException("Error while reading http response", e);
    }
  }

  public static Record buildResponse(
      ODataRequestResult oDataResponse, HttpMethod.ODataVersion oDataVersion) {
    JsonNode responseBody = CommonExecutor.readResponseBody(oDataResponse);
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
    if (oDataVersion.equals(HttpMethod.ODataVersion.V2)) {
      return CommonExecutor.buildV2Response(responseBody, statusCode, countOrInlineCount);
    } else if (oDataVersion.equals(HttpMethod.ODataVersion.V4)) {
      return CommonExecutor.buildV4Response(responseBody, statusCode, countOrInlineCount);
    } else {
      throw new IllegalArgumentException("Unsupported version: " + oDataVersion);
    }
  }

  public static Record buildBatchErrorResponse(
      ODataServiceErrorException error, ODataRequestGeneric bogey) {
    ObjectNode response = JsonNodeFactory.instance.objectNode();
    var m = ConnectorsObjectMapperSupplier.getCopy();
    try {
      var body = error.getHttpBody().get();
      response.put("target", bogey.getRelativeUri().toString()); //> hint the target entity
      response.set("odata", m.readTree(body));
    } catch (JsonProcessingException e) {
      throw new ConnectorException(
          ErrorCodes.GENERIC_ERROR.name(),
          CommonExecutor.buildErrorMsg(e, "OData batch response building error: "),
          e);
    }
    int statusCode = error.getHttpCode();
    return new ODataConnectorResponse(response, statusCode);
  }

  public static Record buildV4Response(
      JsonNode responseBody, int statusCode, Optional<Long> countOrInlineCount) {
    JsonNode value = responseBody.has("value") ? responseBody.get("value") : responseBody;
    return countOrInlineCount.isPresent()
        ? new ODataConnectorResponseWithCount(
            value, statusCode, countOrInlineCount.get().intValue())
        : new ODataConnectorResponse(value, statusCode);
  }

  public static Record buildV2Response(
      JsonNode responseBody, int statusCode, Optional<Long> countOrInlineCount) {
    JsonNode d = responseBody.get("d");
    JsonNode results = d.has("results") ? d.get("results") : d;
    return countOrInlineCount.isPresent()
        ? new ODataConnectorResponseWithCount(
            results, statusCode, countOrInlineCount.get().intValue())
        : new ODataConnectorResponse(results, statusCode);
  }

  public static String buildErrorMsg(Exception e, String prefix) {
    String msg = !prefix.isBlank() ? prefix + e.getMessage() : prefix;
    msg += e.getCause() != null ? " caused by: " + e.getCause().getMessage() : "";
    if (e instanceof ODataServiceErrorException oerr) {
      msg += " caused by: " + oerr.getOdataError();
    }
    return msg;
  }
}
