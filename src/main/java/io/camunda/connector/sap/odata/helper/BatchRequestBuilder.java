package io.camunda.connector.sap.odata.helper;

import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestBatch;
import com.sap.cloud.sdk.datamodel.odata.client.request.UpdateStrategy;
import io.camunda.connector.sap.odata.ODataConnector;
import io.camunda.connector.sap.odata.helper.batchType.BatchRequestRepresentation;
import io.camunda.connector.sap.odata.helper.batchType.Request;
import io.camunda.connector.sap.odata.model.ODataConnectorRequestAccessor;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;

public class BatchRequestBuilder {
  @Getter @Setter private BatchRequestRepresentation[] source;
  private final String oDataService;
  private final ODataProtocol oDataVersion;
  @Getter private ODataRequestBatch batch;

  public BatchRequestBuilder(
      String oDataService,
      ODataProtocol oDataVersion,
      BatchRequestRepresentation[] batchRequestRepresentation) {
    this.oDataService = oDataService;
    this.oDataVersion = oDataVersion;
    this.source = batchRequestRepresentation;
    this.batch = new ODataRequestBatch(oDataService, oDataVersion);
  }

  public void build() {
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
                      ? ODataConnector.createSerializedEntity(request.getPayload())
                      : "";

              // again let's be generous in what we accept, be it "Post" or "post" or "POST"
              String method = request.getMethod().toString().toLowerCase();
              if (method.equalsIgnoreCase(Request.Method.POST.toString().toLowerCase())) {
                CustomODataRequestCreate create =
                    new CustomODataRequestCreate(
                        this.oDataService, resourcePath, payload, this.oDataVersion);
                changeset.addCreate(create);
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
              } else if (method.equalsIgnoreCase(Request.Method.DELETE.toString().toLowerCase())) {
                CustomODataRequestDelete delete =
                    new CustomODataRequestDelete(
                        this.oDataService, resourcePath, null, this.oDataVersion);
                changeset.addDelete(delete);
              } else {
                throw new IllegalArgumentException("Unknown method: " + request.getMethod());
              }
            });
    changeset.endChangeset();
  }
}
