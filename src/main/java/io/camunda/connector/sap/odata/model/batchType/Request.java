package io.camunda.connector.sap.odata.model.batchType;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Request {
  public enum Method {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE");

    private final String value;

    Method(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  private Method method;
  private String resourcePath;
  // optional for GET
  private Options options;
  // only required for POST, PUT, PATCH
  //  private Payload payload;
  private Map<String, Object> payload;
}
