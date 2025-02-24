package io.camunda.connector.sap.odata.helper;

import java.net.URI;

public class Mangler {
  static URI revert(String path) {
    return URI.create(path.replaceAll("%2F", "/"));
  }
}
