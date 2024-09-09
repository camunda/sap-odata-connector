package io.camunda.connector.sap.rfc;

import com.google.gson.JsonObject;
import com.sap.cloud.sdk.cloudplatform.connectivity.*;
import com.sap.cloud.sdk.s4hana.connectivity.exception.RequestExecutionException;
import com.sap.cloud.sdk.s4hana.connectivity.rfc.BapiRequest;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.connector.sap.common.DestinationProvider;
import io.camunda.connector.sap.rfc.model.RFCConnectorRequest;
import java.util.HashMap;

@OutboundConnector(
    name = "SAPRFCOUTBOUNDCONNECTOR",
    inputVariables = {},
    type = "io.camunda:sap:rfc:outbound:1")
@ElementTemplate(
    id = "io.camunda.connector.SAP.rfc.outbound.v1",
    name = "SAP RFC connector",
    version = 1,
    icon = "sap-connector-outbound.svg",
    documentationRef = "https://docs.camunda.io/xxx",
    inputDataClass = RFCConnectorRequest.class)
public class RFCConnector implements OutboundConnectorFunction {
  public Object execute(OutboundConnectorContext context) {
    return executeRequest();
  }

  private Object executeRequest() {
    Destination destination = DestinationProvider.getDestination("test", DestinationType.RFC);
    //    try {
    //      RemoteFunctionCache.clearCache(destination);
    //    } catch (RemoteFunctionException e) {
    //      throw new RuntimeException(e);
    //    }
    var resultList = new HashMap<>();
    try {
      var result =
          new BapiRequest("BAPI_COSTCENTER_GETLIST1")
              .withExporting("CONTROLLINGAREA", "BAPI0012_GEN-CO_AREA", "1000")
              .withTableAsReturn("BAPIRET2")
              .execute(destination);

      int i = 0;
      for (var entry : result.get("COSTCENTERLIST").getAsCollection()) {
        var _entry = new HashMap<>();
        entry
            .getAsObject()
            .as(JsonObject.class)
            .entrySet()
            .forEach(e -> _entry.put(e.getKey(), e.getValue()));
        resultList.put(i++, _entry);
      }
    } catch (final RequestExecutionException e) {
      e.printStackTrace();
    }
    return resultList;
  }
}
