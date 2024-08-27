package io.camunda.sap_integration;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class InputValidationTest {

  static Stream<Object> validPayloadProvider() {
    return Stream.of(
        "{\"withBrackets\":\"payload\"}",
        "\"valid\":\"payload\"",
        "\"number\":1",
        "\"array\": [1,2,3]",
        "\"boolean\":true",
        "\"white space\": \"value\"",
        "\"key1\": {\"key2\": \"value2\", \"key3\": {\"key4\": \"value4\"}}, \"key5\": [1, 2, {\"key6\": \"value6\"}]",
        new JSONObject().put("jsonobject", "payload")
    );
  }

  static Stream<Object> invalidPayloadProvider() {
    return Stream.of(
        "unqouted: property",
        "{invalid:true}",
        "{missing: \"closing bracket\""
    );
  }


  @ParameterizedTest(name = "is {0}")
  @MethodSource("validPayloadProvider")
  void valid_payload(Object argument) {
    var input = new JSONObject()
        .put("tpl_Payload", argument);

    var context = OutboundConnectorContextBuilder.create()
        .variables(input.toString())
        .build();

    var function = new SAPconnector();

    assertDoesNotThrow(() -> function.validateInput(context));
  }


  @ParameterizedTest(name = "is {0}")
  @MethodSource("invalidPayloadProvider")
  void invalid_payload(Object argument) {
    var input = new JSONObject()
        .put("tpl_Payload", argument);

    var context = OutboundConnectorContextBuilder.create()
        .variables(input.toString())
        .build();

    var function = new SAPconnector();

    ConnectorException exception = assertThrowsExactly(ConnectorException.class, () -> function.validateInput(context));
    assertThat(exception.getMessage()).contains("invalid JSON payload");
  }
}