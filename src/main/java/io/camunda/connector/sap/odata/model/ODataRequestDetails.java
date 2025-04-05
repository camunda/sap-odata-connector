package io.camunda.connector.sap.odata.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "requestType")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ODataRequestDetails.BatchRequest.class, name = "batchReq"),
  @JsonSubTypes.Type(value = ODataRequestDetails.SimpleRequest.class, name = "simpleReq")
})
@TemplateDiscriminatorProperty(
    group = "sap",
    label = "Request type",
    name = "requestType",
    defaultValue = "simpleReq")
public sealed interface ODataRequestDetails {

  @TemplateSubType(id = "batchReq", label = "Batch Request")
  record BatchRequest(
      @TemplateProperty(
              group = "batch",
              label = "OData Version",
              description = "OData version to use for the batch request",
              defaultValue = "V2")
          HttpMethod.ODataVersion oDataVersion,
      @TemplateProperty(
              group = "batch",
              label = "Batch Request Payload",
              feel = FeelMode.required,
              type = TemplateProperty.PropertyType.Text,
              description = "Provide the payload for the batch request")
          @NotEmpty
          List<Map<String, Object>> batchRequestPayload)
      implements ODataRequestDetails {}

  @TemplateSubType(id = "simpleReq", label = "OData Request")
  record SimpleRequest(
      @TemplateProperty(
              group = "sap",
              label = "OData Entity/-Set",
              description =
                  "query target (e.g. bike(12) ), can also contain navigation properties\n( e.g. bike('12')/toWheels/toBolts )",
              feel = FeelMode.optional)
          @Pattern(regexp = "^[^/].*$", message = "entityOrEntitySet must not start with a '/'")
          @NotEmpty
          String entityOrEntitySet,
      @Valid HttpMethod httpMethod,
      @TemplateProperty(
              label = "Request body",
              description = "Payload to send with the request",
              feel = FeelMode.optional,
              group = "sap",
              optional = true,
              defaultValue = "={}",
              condition =
                  @TemplateProperty.PropertyCondition(
                      property = "requestDetails.httpMethod.httpMethod",
                      oneOf = {"post", "put", "patch"}))
          Map<String, Object> payload)
      implements ODataRequestDetails {}
}
