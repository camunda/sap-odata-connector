package io.camunda.connector.sap.odata.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestBatch;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestGeneric;
import com.sap.cloud.sdk.datamodel.odata.client.request.UpdateStrategy;
import io.camunda.connector.sap.odata.ODataRequestExecutor;
import io.camunda.connector.sap.odata.helper.CustomODataRequestCreate;
import io.camunda.connector.sap.odata.helper.CustomODataRequestDelete;
import io.camunda.connector.sap.odata.helper.CustomODataRequestRead;
import io.camunda.connector.sap.odata.helper.CustomODataRequestUpdate;
import io.camunda.connector.sap.odata.model.batchType.BatchRequestRepresentation;
import io.camunda.connector.sap.odata.model.batchType.Request;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

public class BatchRequestBuilder {
  @Getter @Setter private String oDataService;
  @Getter @Setter private ODataProtocol oDataVersion;

  @Setter private ObjectMapper mapper;
  @Getter @Setter private BatchRequestRepresentation[] source;

  // we need to keep track of the individual requests for the batch
  // for later retrieving the results programmatically
  @Getter @Setter private ArrayList<ODataRequestGeneric> requests = new ArrayList<>();
  @Getter private ODataRequestBatch batch;

  private ObjectMapper setDefaultMapper() {
    ObjectMapper m =
        JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES, true)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();
    this.mapper = m;
    return m;
  }

  public ObjectMapper getMapper() {
    return Optional.ofNullable(this.mapper).orElseGet(this::setDefaultMapper);
  }

  public BatchRequestBuilder buildSource(String batch) throws JsonProcessingException {
    ObjectMapper mapper = Optional.ofNullable(this.mapper).orElseGet(this::setDefaultMapper);
    JsonNode json = mapper.readTree(batch);
    this.source = mapper.convertValue(json, BatchRequestRepresentation[].class);
    return this;
  }

  public void buildRequest() {
    if (this.oDataService == null || this.oDataVersion == null) {
      throw new IllegalArgumentException(
          "Set the OData service and version first via setODataService(String oDataService) and setODataVersion(ODataProtocol oDataVersion)!");
    }
    this.batch = new ODataRequestBatch(oDataService, oDataVersion);

    if (this.source == null) {
      throw new IllegalArgumentException(
          "Set the batch request source first via buildRepresentation(String batch)!");
    }
    for (BatchRequestRepresentation batchRequestRepresentation : source) {
      // we're generous in what we accept, be it "Batch" or "batch" or "BATCH"
      if (batchRequestRepresentation
          .getType()
          .toString()
          .equalsIgnoreCase(BatchRequestRepresentation.EntryKind.BATCH.toString())) {
        addBatch(batchRequestRepresentation);
      } else if (batchRequestRepresentation
          .getType()
          .toString()
          .equalsIgnoreCase(BatchRequestRepresentation.EntryKind.CHANGESET.toString())) {
        addChangeSet(batchRequestRepresentation);
      } else {
        throw new IllegalArgumentException(
            "Unknown batch type: " + batchRequestRepresentation.getType());
      }
    }
  }

  private void addBatch(BatchRequestRepresentation batchRequestRepresentation) {
    Arrays.stream(batchRequestRepresentation.getRequests())
        .forEach(
            request -> {
              CustomODataRequestRead read =
                  new CustomODataRequestRead(
                      this.oDataService, request.getResourcePath(), "", this.oDataVersion);
              if (request.getOptions() != null) {
                ODataConnectorRequestAccessor.queryParams(request.getOptions().asMap())
                    .forEach(read::addQueryParameter);
              }

              this.batch.addRead(read);
              this.requests.add(read); //> keep track for later result retrieval
            });
  }

  private void addChangeSet(BatchRequestRepresentation batchRequestRepresentation) {
    ODataRequestBatch.Changeset changeset = this.batch.beginChangeset();
    Arrays.stream(batchRequestRepresentation.getRequests())
        .forEach(
            request -> {
              // there is always an entity referenced in each write op
              ODataResourcePath resourcePath = ODataResourcePath.of(request.getResourcePath());
              // DELETE typically does not have a payload, but the all other http verbs
              String payload =
                  request.getPayload() != null
                      ? ODataRequestExecutor.createSerializedEntity(request.getPayload())
                      : "";

              // again let's be generous in what we accept, be it "Post" or "post" or "POST"
              String method = request.getMethod().toString().toLowerCase();
              if (method.equalsIgnoreCase(Request.Method.POST.toString().toLowerCase())) {
                CustomODataRequestCreate create =
                    new CustomODataRequestCreate(
                        this.oDataService, resourcePath, payload, this.oDataVersion);
                changeset.addCreate(create);
                this.requests.add(create); //> keep track for later result retrieval
              } else if (method.equalsIgnoreCase(Request.Method.PUT.toString().toLowerCase())) {
                CustomODataRequestUpdate update =
                    new CustomODataRequestUpdate(
                        this.oDataService,
                        resourcePath,
                        payload,
                        UpdateStrategy.REPLACE_WITH_PUT,
                        null,
                        this.oDataVersion);
                changeset.addUpdate(update);
                this.requests.add(update); //> keep track for later result retrieval
              } else if (method.equalsIgnoreCase(Request.Method.PATCH.toString().toLowerCase())) {
                CustomODataRequestUpdate update =
                    new CustomODataRequestUpdate(
                        this.oDataService,
                        resourcePath,
                        payload,
                        UpdateStrategy.MODIFY_WITH_PATCH,
                        null,
                        this.oDataVersion);
                changeset.addUpdate(update);
                this.requests.add(update); //> keep track for later result retrieval
              } else if (method.equalsIgnoreCase(Request.Method.DELETE.toString().toLowerCase())) {
                CustomODataRequestDelete delete =
                    new CustomODataRequestDelete(
                        this.oDataService, resourcePath, null, this.oDataVersion);
                changeset.addDelete(delete);
                this.requests.add(delete); //> keep track for later result retrieval
              } else {
                throw new IllegalArgumentException("Unknown method: " + request.getMethod());
              }
            });
    changeset.endChangeset();
  }
}
