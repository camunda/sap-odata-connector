package io.camunda.connector.sap.helper;

import java.net.URI;

public class Mangler {
  static URI revert(String path) {
    return URI.create(path.replaceAll("%2F", "/"));
  }
}
