package io.camunda.connector.sap;

import static io.camunda.connector.sap.model.ODataConnectorRequest.ODataVersion.V4;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.camunda.connector.sap.helper.CustomODataRequestCreate;
import io.camunda.connector.sap.helper.CustomODataRequestDelete;
import io.camunda.connector.sap.helper.CustomODataRequestRead;
import io.camunda.connector.sap.helper.CustomODataRequestUpdate;
import io.camunda.connector.sap.model.ODataConnectorRequest;
import io.camunda.connector.sap.model.ODataConnectorResponse;
import io.camunda.connector.sap.model.ODataConnectorResponseWithCount;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ODataS4Test {

  @Nested
  @Disabled("S/4 backend not yet fully customized for this")
  class validate_reconstructions_in_S4 {
    // full use case: check entry, create a new one, recheck it, update it, recheck it, delete it,
    // recheck it

    @Test
    void GET() {}

    @Test
    void POST() {}

    @Test
    void GET_validate_POST() {}

    @Test
    void PATCH() {}

    @Test
    void GET_validate_PATCH() {}

    @Test
    void DELETE() {}

    @Test
    void GET_validate_DELETE() {}
  }

  @Nested
  class count_in_v4 {
    static String oDataService =
        "/sap/opu/odata4/sap/api_materialserialnumber/srvd_a2x/sap/materialserialnumber/0001/";
    static String entity = "MaterialSerialNumber(Material='2261',SerialNumber='10002973')";
    static String entitySet = "MaterialSerialNumber";

    @Test
    void count_not_for_entity_in_v4() {
      var input =
          new ODataConnectorRequest(
              "s4",
              oDataService,
              entity,
              new ODataConnectorRequest.HttpMethod.Get(
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  new ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V4(null, false)),
              null);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var req =
          (CustomODataRequestRead)
              function.buildRequest(context.bindVariables((ODataConnectorRequest.class)));
      var response = function.execute(context);

      assertThat(req.getRelativeUri().toString()).isEqualTo(oDataService + entity);
      assertThat(((ODataConnectorResponse) response).result().get("Material").asText())
          .isEqualTo("2261");
      assertThat(((ODataConnectorResponse) response).result().get("SerialNumber").asText())
          .isEqualTo("10002973");
      assertThat(((ODataConnectorResponse) response).result().get("Equipment").asText())
          .isEqualTo("10002973");
      assertThat(((ODataConnectorResponse) response).result().get("EquipmentCategory").asText())
          .isEqualTo("P");
    }

    @Test
    void allow_count_in_entitysets_v4() {
      var input =
          new ODataConnectorRequest(
              "s4",
              oDataService,
              entitySet,
              new ODataConnectorRequest.HttpMethod.Get(
                  null,
                  5L,
                  null,
                  null,
                  null,
                  null,
                  new ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V4(null, true)),
              null);
      var inputWithoutCount =
          new ODataConnectorRequest(
              "s4",
              oDataService,
              entitySet,
              new ODataConnectorRequest.HttpMethod.Get(
                  null,
                  5L,
                  null,
                  null,
                  null,
                  null,
                  new ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V4(null, false)),
              null);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();
      var contextWithoutCount =
          OutboundConnectorContextBuilder.create().variables(inputWithoutCount).build();

      var function = new ODataConnector();
      var response = function.execute(context);
      var responseWithoutCount = function.execute(contextWithoutCount);

      assertThat(((ODataConnectorResponseWithCount) response).countOrInlineCount())
          .isGreaterThanOrEqualTo(5);
      assertThat(((ODataConnectorResponseWithCount) response).result().size()).isEqualTo(5);

      assertThat(((ODataConnectorResponse) responseWithoutCount).result().size()).isEqualTo(5);
      assertThat(
              Arrays.stream(responseWithoutCount.getClass().getDeclaredMethods())
                  .noneMatch(method -> method.getName().contains("countOrInlineCount")))
          .isTrue();
    }
  }

  @Nested
  class reconstruct_v4_entity_reference {

    // test these boundary conditions:
    // - OData v4 CRU(put + patch)D with key referenced in REST manner
    // fex: /WarrantyClaim/{WrntyClaimHeaderUUID}
    // https://api.sap.com/api/WARRANTYCLAIM_0001/path/get_WarrantyClaim__WrntyClaimHeaderUUID_

    // - OData v4 bound action with SAP__self.<action> in path
    // fex: /WarrantyClaim/{WrntyClaimHeaderUUID}/SAP__self.ProcessSupplierClaim

    static String path = "/sap/opu/odata4/sap/api_purchaseorder_2/srvd_a2x/sap/purchaseorder/0001";
    static ODataConnectorRequest.HttpMethod _get =
        new ODataConnectorRequest.HttpMethod.Get(
            null,
            null,
            null,
            null,
            null,
            null,
            new ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V4(null, null));
    static ODataConnectorRequest.HttpMethod _post = new ODataConnectorRequest.HttpMethod.Post(V4);
    static ODataConnectorRequest.HttpMethod _put = new ODataConnectorRequest.HttpMethod.Put(V4);
    static ODataConnectorRequest.HttpMethod _delete =
        new ODataConnectorRequest.HttpMethod.Delete(V4);

    static String[] _paths = {
      "PurchaseOrder/4500000001/SAP__self.ProcessSupplierClaim",
      "PurchaseOrder('4500000001')/SAP__self.ProcessSupplierClaim",
      "PurchaseOrderItem/4500000001/1/_PurchaseOrder",
      "PurchaseOrderItem(PurchaseOrder='4500000001',PurchaseOrderItem='1')/_PurchaseOrder)"
    };

    static List<Arguments> v4_get =
        Arrays.stream(_paths).map(path -> arguments(path, _get)).toList();
    static List<Arguments> v4_post =
        Arrays.stream(_paths).map(path -> arguments(path, _post, Map.of("foo", "bar"))).toList();
    static List<Arguments> v4_put =
        Arrays.stream(_paths).map(path -> arguments(path, _put, Map.of("update", "foo"))).toList();
    static List<Arguments> v4_delete =
        Arrays.stream(_paths).map(path -> arguments(path, _delete)).toList();

    @ParameterizedTest
    @FieldSource({"v4_get"})
    void for_GET(String entityPath, ODataConnectorRequest.HttpMethod method) {

      var input = new ODataConnectorRequest("notRelevant", path, entityPath, method, null);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var req =
          (CustomODataRequestRead)
              function.buildRequest(context.bindVariables((ODataConnectorRequest.class)));

      assertThat(req.getRelativeUri().toString()).isEqualTo(path + "/" + entityPath);
    }

    @ParameterizedTest
    @FieldSource({"v4_post"})
    void for_POST(
        String entityPath, ODataConnectorRequest.HttpMethod method, Map<String, Object> payload) {
      var input = new ODataConnectorRequest("notRelevant", path, entityPath, method, payload);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var req =
          (CustomODataRequestCreate)
              function.buildRequest(context.bindVariables((ODataConnectorRequest.class)));

      assertThat(req.getRelativeUri().toString()).isEqualTo(path + "/" + entityPath);
    }

    @ParameterizedTest
    @FieldSource({"v4_put"})
    void for_PUT(
        String entityPath, ODataConnectorRequest.HttpMethod method, Map<String, Object> payload) {
      var input = new ODataConnectorRequest("notRelevant", path, entityPath, method, payload);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var req =
          (CustomODataRequestUpdate)
              function.buildRequest(context.bindVariables((ODataConnectorRequest.class)));

      assertThat(req.getRelativeUri().toString()).isEqualTo(path + "/" + entityPath);
    }

    @ParameterizedTest
    @FieldSource({"v4_delete"})
    void for_DELETE(String entityPath, ODataConnectorRequest.HttpMethod method) {
      var input = new ODataConnectorRequest("notRelevant", path, entityPath, method, null);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var req =
          (CustomODataRequestDelete)
              function.buildRequest(context.bindVariables((ODataConnectorRequest.class)));

      assertThat(req.getRelativeUri().toString()).isEqualTo(path + "/" + entityPath);
    }
  }
}
