package io.camunda.connector.sap.odata.model;

import static java.net.URLEncoder.encode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ODataConnectorRequestAccessor {
  public static Map<String, String> queryParams(HttpMethod.Get get) {
    Map<String, String> params = new HashMap<>();
    putIfPresent(params, "$top", get.top(), String::valueOf);
    putIfPresent(params, "$filter", get.filter());
    putIfPresent(params, "$skip", get.skip(), String::valueOf);
    putIfPresent(params, "$orderby", get.orderby());
    putIfPresent(params, "$expand", get.expand());
    putIfPresent(params, "$select", get.select());
    switch (get.oDataVersionGet()) {
      case HttpMethod.Get.ODataVersionGet.V2 v2 -> {
        if (v2.inlinecount() != null && v2.inlinecount()) {
          putIfPresent(params, "$inlinecount", true, (ignored) -> "allpages");
        }
      }
      case HttpMethod.Get.ODataVersionGet.V4 v4 -> {
        if (v4.count() != null && v4.count()) {
          putIfPresent(params, "$count", true, (ignored) -> "true");
        }
        putIfPresent(params, "$search", v4.search());
      }
    }
    return params;
  }

  public static Map<String, String> queryParams(Map<String, String> getParams) {
    Map<String, String> params = new HashMap<>();
    getParams.forEach(
        (key, value) -> {
          if (Set.of(
                  "$format",
                  "$top",
                  "$skip",
                  "$filter",
                  "$orderby",
                  "$expand",
                  "$select",
                  "$inlinecount",
                  "$count",
                  "$search")
              .contains(key)) {
            putIfPresent(params, key, value);
          }
        });
    return params;
  }

  private static void putIfPresent(Map<String, String> params, String key, String value) {
    if (value != null && !value.isEmpty()) {
      if (Set.of("$filter", "$expand", "$select", "$search").contains(key)) {
        value = encode(value, StandardCharsets.UTF_8);
      } else if (key.equals("$orderby")) {
        //> special handling: .encode does " " -> "+", in the orderby clause, we need "%20"
        value = encode(value, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
      }
      params.put(key, value);
    }
  }

  private static void putIfPresent(
      Map<String, String> params, String key, Object value, Function<Object, String> mapper) {
    if (value != null) {
      putIfPresent(params, key, mapper.apply(value));
    }
  }

  public static HttpMethod.ODataVersion oDataVersion(
      ODataRequestDetails.SimpleRequest requestDetails) {
    return switch (requestDetails.httpMethod()) {
      case HttpMethod.Get get ->
          switch (get.oDataVersionGet()) {
            case HttpMethod.Get.ODataVersionGet.V2 ignored -> HttpMethod.ODataVersion.V2;
            case HttpMethod.Get.ODataVersionGet.V4 ignored -> HttpMethod.ODataVersion.V4;
          };
      case HttpMethod.Delete delete -> delete.oDataVersionDelete();
      case HttpMethod.Patch patch -> patch.oDataVersionPatch();
      case HttpMethod.Post post -> post.oDataVersionPost();
      case HttpMethod.Put put -> put.oDataVersionPut();
    };
  }

  public static HttpMethod.ODataVersion oDataVersion(
      ODataRequestDetails.BatchRequest requestDetails) {
    return requestDetails.oDataVersion();
  }
}
