package io.camunda.sap_integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.*;
import io.camunda.connector.api.json.ConnectorsObjectMapperSupplier;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ODataRequest {

  private final String destination;
  private final String servicePath;
  private final String entityOrEntitySet;
  private final HashMap<String, String> queryParameter;
  private final ODataProtocol oDataVersion;

  private final Destination runtimeDestination;
  private final HttpClient client;

  public static Map<String, String> defaultResponse = Map.of("result", "NOK");

  private static final Logger LOGGER = LoggerFactory.getLogger(ODataRequest.class);

  public ODataRequest(String destination, String servicePath, String entityOrEntitySet, HashMap<String, String> queryParameter, ODataProtocol version) {

    this.destination = destination;
    this.servicePath = servicePath;
    this.entityOrEntitySet = entityOrEntitySet;
    this.queryParameter = queryParameter;
    this.oDataVersion = version;

    this.runtimeDestination = build();
    this.client = HttpClientAccessor.getHttpClient(this.runtimeDestination);
  }

  private Destination build() {
    // TODO: map "trustAllCertificates" to an element template option
    return DestinationAccessor.getDestination(this.destination);
  }

  Object get() {
    ODataRequestRead request = new ODataRequestRead(
        this.servicePath,
        this.entityOrEntitySet,
        "", // encodedQuery
        this.oDataVersion);
    this.queryParameter.forEach(request::addQueryParameter);

    ODataRequestResultGeneric _result = request.execute(this.client);
    Map result = mapResponseToProtocol(_result);

    LOGGER.debug("//> response {}", result);
    return result;
  }



  Object post(String payload) {
    ODataRequestCreate request = new ODataRequestCreate(
        this.servicePath,
        this.entityOrEntitySet,
        payload,
        this.oDataVersion);

    ODataRequestResultGeneric _result = request.execute(this.client);
    Map result = mapResponseToProtocol(_result);

    LOGGER.debug("//> response {}", result);
    return result;
  }

 Object putOrPatch(UpdateStrategy putOrPatch, ODataResourcePath resourcePath, String payload) {
    ODataRequestUpdate request = new ODataRequestUpdate(this.servicePath, resourcePath, payload, putOrPatch, null, this.oDataVersion);
    ODataRequestResultGeneric _result = request.execute(this.client);

    var result = mapResponseToProtocol(_result);
    LOGGER.debug("//> response {}", result);

    return result;
  }

  Object delete() {
    ODataResourcePath path = ODataResourcePath.of(this.entityOrEntitySet);
    ODataRequestDelete request = new ODataRequestDelete(this.servicePath, path, null, this.oDataVersion);
    ODataRequestResultGeneric _result = request.execute(this.client);


    int statusCode = _result.getHttpResponse().getStatusLine().getStatusCode();
    var result = Map.of("result", /* response body of http delete is always empty */ "{}", "statusCode", statusCode);
    LOGGER.debug("//> response {}", result);

    return result;
  }

  private Map mapResponseToProtocol(ODataRequestResult result) {
    HttpResponse httpResponse = result.getHttpResponse();
    HttpEntity entity = httpResponse.getEntity();
    InputStream responseBody = null;
    Map jsonResponse = null;
    try {
      responseBody = entity.getContent();
      ObjectMapper mapper = ConnectorsObjectMapperSupplier.DEFAULT_MAPPER;
      jsonResponse = mapper.readValue(responseBody, Map.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Map mappedResult;
    int statusCode = httpResponse.getStatusLine().getStatusCode();
    if (this.oDataVersion.equals(ODataProtocol.V2)) {
      Map d = (Map) jsonResponse.get("d");
      mappedResult = d.containsKey("results") ? //> entityset
          Map.of("result", d.get("results"), "statusCode", statusCode) :
          Map.of("result", d, "statusCode", statusCode); //> entity
    } else if (this.oDataVersion.equals(ODataProtocol.V4) && jsonResponse.containsKey("value")) {
      mappedResult = Map.of("result", jsonResponse.get("value"), "statusCode", statusCode); //> entityset
    } else {
      mappedResult = Map.of("result", jsonResponse, "statusCode", statusCode); //> entity
    }
    return mappedResult;
  }
}
