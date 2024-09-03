package io.camunda.sap_integration.model;

import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Delete;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Get;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Get.ODataVersionGet.V2;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Get.ODataVersionGet.V4;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Patch;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Post;
import io.camunda.sap_integration.model.SAPConnectorRequest.HttpMethod.Put;
import io.camunda.sap_integration.model.SAPConnectorRequest.ODataVersion;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SAPConnectorRequestAccessor {
  public static Map<String, String> queryParams(Get get) {
    Map<String, String> params = new HashMap<>();
    putIfPresent(params, "$top", get.top(), String::valueOf);
    putIfPresent(params, "$filter", get.filter());
    putIfPresent(params, "$skip", get.skip(), String::valueOf);
    putIfPresent(params, "$orderBy", get.orderBy());
    putIfPresent(params, "$expand", get.expand());
    putIfPresent(params, "$select", get.select());
    switch (get.oDataVersionGet()) {
      case V2 ignored -> {
        if (get.count() != null && get.count()) {
          putIfPresent(params, "$inlinecount", "allpages");
        }
      }
      case V4 v4 -> {
        putIfPresent(params, "$inlinecount", v4.inlinecount(), String::valueOf);
        putIfPresent(params, "$count", get.count(), String::valueOf);
        putIfPresent(params, "$search", v4.search());
      }
    }
    return params;
  }

  private static void putIfPresent(Map<String, String> params, String key, String value) {
    if (value != null && !value.isEmpty()) {
      params.put(key, value);
    }
  }

  private static void putIfPresent(
      Map<String, String> params, String key, Object value, Function<Object, String> mapper) {
    if (value != null) {
      putIfPresent(params, key, mapper.apply(value));
    }
  }

  public static ODataVersion oDataVersion(SAPConnectorRequest request) {
    return switch (request.httpMethod()) {
      case Get get ->
          switch (get.oDataVersionGet()) {
            case V2 ignored -> ODataVersion.V2;
            case V4 ignored -> ODataVersion.V4;
          };
      case Delete delete -> delete.oDataVersionDelete();
      case Patch patch -> patch.oDataVersionPatch();
      case Post post -> post.oDataVersionPost();
      case Put put -> put.oDataVersionPut();
    };
  }
}
