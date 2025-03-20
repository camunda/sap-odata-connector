package io.camunda.connector.sap.odata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import io.camunda.connector.sap.odata.model.BatchRequestBuilder;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BatchBuildingTest {

  String testFileContent;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    testFileContent = Files.readString(Paths.get("src/test/resources/batch1.json"));
  }

  @Test
  @SneakyThrows
  void buildBatchRepresentation() {

    //    ObjectMapper mapper =
    //        JsonMapper.builder()
    //            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    //            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, true)
    //            .serializationInclusion(JsonInclude.Include.NON_NULL)
    //            .build();
    //    JsonNode jsonWithoutRootNode = mapper.readTree(content).at("/batch");
    //    var batchRepresentation =
    //        mapper.convertValue(jsonWithoutRootNode, BatchRequestRepresentation[].class);

    var batchRequest = new BatchRequestBuilder();
    var mapper = batchRequest.getMapper();

    // use the built-in mapper to crop the root node
    //    JsonNode jsonWithoutRootNode = mapper.readTree(testFileContent).at("/batch");
    //    String testFileContentWithoutRootNode = jsonWithoutRootNode.toString();

    var batchRepresentation = batchRequest.buildSource(testFileContent).getSource();
    String serializedContent = mapper.writeValueAsString(batchRepresentation);

    var actual = mapper.readTree(testFileContent.toLowerCase());
    var excepted = mapper.readTree(serializedContent.toLowerCase());
    assertTrue(actual.equals(excepted));
  }

  @Test
  @SneakyThrows
  void BuildBatchRequest() {
    BatchRequestBuilder builder = new BatchRequestBuilder();
    builder.setODataVersion(ODataProtocol.V2);
    builder.setODataService("/sap/opu/odata/sap/API_BUSINESS_PARTNER");
    builder.buildSource(testFileContent).buildRequest();

    var batchRequest = builder.getBatch();
    assertEquals(batchRequest.getRequests().size(), 5);
  }
}
