package io.camunda.connector.sap.odata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataServiceErrorException;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestGeneric;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResultGeneric;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestResultMultipartGeneric;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.sap.odata.helper.CommonExecutor;
import io.camunda.connector.sap.odata.model.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ODataBatchRequestExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ODataBatchRequestExecutor.class);

  public Object executeBatch(ODataConnectorRequest request) {
    Destination destination = CommonExecutor.buildDestination(request.destination());
    LOGGER.debug("Destination: {}", destination);
    HttpClient httpClient = HttpClientAccessor.getHttpClient(destination);

    ODataProtocol protocol =
        CommonExecutor.determineProtocol(
            ODataConnectorRequestAccessor.oDataVersion(
                (ODataRequestDetails.BatchRequest) request.requestDetails()));

    BatchRequestBuilder builder = new BatchRequestBuilder();
    builder.setODataVersion(protocol);
    builder.setODataService(request.oDataService());

    try {
      var payloadAsString =
          builder
              .getMapper()
              .writeValueAsString(
                  ((ODataRequestDetails.BatchRequest) request.requestDetails())
                      .batchRequestPayload());
      builder.buildSource(payloadAsString).buildRequest();

      LOGGER.debug(
          "OData $batch to: {}",
          builder.getODataService()
              + " - incl requests: "
              + builder.getBatch().getRequests().size());

      LOGGER.debug(
          "OData $batch start at: "
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")));
      ODataRequestResultMultipartGeneric batchResult = builder.getBatch().execute(httpClient);
      LOGGER.debug(
          "OData $batch finished at: "
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")));

      return buildBatchResponse(builder, batchResult, protocol);
    } catch (JsonProcessingException e) {
      throw new ConnectorException(
          ErrorCodes.GENERIC_ERROR.name(),
          CommonExecutor.buildErrorMsg(e, "OData $batch runtime error: "),
          e);

    } catch (ODataResponseException e) {
      throw new ConnectorException(
          String.valueOf(e.getHttpCode()),
          CommonExecutor.buildErrorMsg(e, "OData $batch response error: "),
          e);
    } catch (RuntimeException e) {
      throw new ConnectorException(
          ErrorCodes.BATCH_SERVICE_ERROR.name(),
          CommonExecutor.buildErrorMsg(e, "OData $batch service error: "),
          e);
    }
  }

  private Record buildBatchResponse(
      BatchRequestBuilder builder,
      ODataRequestResultMultipartGeneric batchResult,
      ODataProtocol oDataVersion)
      throws JsonProcessingException {
    var originalRequests = builder.getRequests();
    var responses = new ArrayList<>();
    // individual requests inside a $batch may error out,
    // yet the overall $batch is successful
    for (ODataRequestGeneric originalRequest : originalRequests) {
      ODataRequestResultGeneric result = null;
      Record response = null;
      try {
        result = batchResult.getResult(originalRequest);
        response =
            CommonExecutor.buildResponse(
                result,
                oDataVersion.getProtocolVersion().equals("V2")
                    ? HttpMethod.ODataVersion.V2
                    : HttpMethod.ODataVersion.V4);
      } catch (ODataServiceErrorException e) {
        // getting the batch result via the original request throws
        // if the original request != 20x,
        response = CommonExecutor.buildBatchErrorResponse(e, originalRequest);
      }
      responses.add(response);
    }

    return new ODataConnectorBatchResponse(responses);
  }
}
