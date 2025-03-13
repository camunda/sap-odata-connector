package io.camunda.connector.sap.odata.model.refined;

import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

@TemplateDiscriminatorProperty(
    group = "sap",
    label = "Request type",
    name = "requestType",
    defaultValue = "simpleReq",
    description = "Whether the request is a batch request or not")
public sealed interface ODataRequestDetails {

  @TemplateSubType(id = "batchReq", label = "Batch Request")
  record BatchRequest(
      @TemplateProperty(
          group = "batch",
          label = "Batch Request Payload",
          feel = FeelMode.optional,
          optional = true,
          type = TemplateProperty.PropertyType.Text,
          description = "Provide the payload for the batch request")
      String batchRequestPayload
  ) implements ODataRequestDetails {}

  @TemplateSubType(id = "simpleReq", label = "Simple Request")
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

      @TemplateProperty(
          label = "Request body",
          description = "Payload to send with the request",
          feel = FeelMode.optional,
          group = "sap",
          optional = true,
          defaultValue = "={}",
          condition = @TemplateProperty.PropertyCondition(
              property = "httpMethod",
              oneOf = {"post", "put", "patch"}))
      Map<String, Object> payload,

      @Valid ODataConnectorRequest.HttpMethod httpMethod

  ) implements ODataRequestDetails {}
}
