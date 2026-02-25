package io.camunda.connector.sap.odata;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class BaseTest {

  protected static final String HOTFIX_CASES_PATH = "src/test/resources/hotfix.json";

  // Actual values that secrets resolve to
  public interface ActualValue {
    String DESTINATION = "s4-test";
  }

  // Secret placeholder keys — must match {{secrets.XXX}} references in hotfix.json
  protected interface SecretsConstant {
    String DESTINATION = "my_destination";
  }

  protected static Stream<String> hotfixTestCases() throws IOException {
    return loadTestCasesFromResourceFile(HOTFIX_CASES_PATH);
  }

  /**
   * Loads test cases from a JSON file. Supports both a single JSON object and an array of objects.
   */
  @SuppressWarnings("unchecked")
  protected static Stream<String> loadTestCasesFromResourceFile(final String fileWithTestCasesUri)
      throws IOException {
    final String cases = readString(new File(fileWithTestCasesUri).toPath(), UTF_8);
    final ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(cases);

    List<Object> items;
    if (root.isArray()) {
      items = mapper.readValue(cases, ArrayList.class);
    } else {
      items = List.of(mapper.readValue(cases, Object.class));
    }

    return items.stream()
        .map(
            value -> {
              try {
                return mapper.writeValueAsString(value);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
  }
}
