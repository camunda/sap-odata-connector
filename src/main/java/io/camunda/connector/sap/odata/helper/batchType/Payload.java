package io.camunda.connector.sap.odata.helper.batchType;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter @Setter
public class Payload {
  private Map<String, Object> content;
}
