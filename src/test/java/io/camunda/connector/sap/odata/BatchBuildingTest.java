package io.camunda.connector.sap.odata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.camunda.connector.sap.odata.helper.batchType.BatchRequestRepresentation;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchBuildingTest {
  @Test
  @SneakyThrows
  void buildBatch() {
    // read in file batch.json from resource path
    String content = Files.readString(Paths.get("src/test/resources/batch.json"));

    ObjectMapper mapper =
        JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, true)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();
    JsonNode jsonWithoutRootNode = mapper.readTree(content).at("/batch");
    var batchRepresentation =
        mapper.convertValue(jsonWithoutRootNode, BatchRequestRepresentation[].class);

    // Serialize batchRepresentation back to JSON
    String serializedContent = mapper.writeValueAsString(batchRepresentation);

    // Compare the original JSON content with the serialized content
    assertTrue(jsonWithoutRootNode.toString().equalsIgnoreCase(serializedContent));
  }
}
