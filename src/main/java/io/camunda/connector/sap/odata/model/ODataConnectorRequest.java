package io.camunda.connector.sap.odata.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.java.annotation.NestedProperties;
import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

public record ODataConnectorRequest(
    @TemplateProperty(
            group = "sap",
            label = "BTP destination name",
            description = "BTP destination pointing to the SAP System to connect to (e.g. a4h)",
            feel = FeelMode.optional)
        @NotEmpty
        String destination,
    @TemplateProperty(
            group = "sap",
            label = "OData base service path",
            description = "absolute base path, e.g. /sap/opu/odata/dmo/case",
            feel = FeelMode.optional)
        @Pattern(regexp = "^([/=]).*", message = "oDataService must start with a '/'")
        @NotEmpty
        String oDataService,
    @TemplateProperty(
            group = "sap",
            label = "OData Entity/-Set",
            description =
                "query target (e.g. bike(12) ), can also contain navigation properties\n( e.g. bike('12')/toWheels/toBolts )",
            condition =
                @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
            feel = FeelMode.optional)
        @Pattern(regexp = "^[^/].*$", message = "entityOrEntitySet must not start with a '/'")
        @NotEmpty
        String entityOrEntitySet,
    @Valid
        @NestedProperties(
            condition =
                @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"))
        HttpMethod httpMethod,
    @TemplateProperty(
            label = "Request body",
            description = "Payload to send with the request",
            feel = FeelMode.optional,
            group = "sap",
            optional = true,
            defaultValue = "={}",
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "",
                    allMatch = {
                      @TemplateProperty.NestedPropertyCondition(
                          property = "batchReq",
                          equals = "=false"),
                      @TemplateProperty.NestedPropertyCondition(
                          property = "httpMethod.httpMethod",
                          oneOf = {"post", "put", "patch"})
                    }))
        Map<String, Object> payload,
    @TemplateProperty(
            id = "batchReq",
            group = "batch",
            label = "$batch",
            optional = true,
            description = "Indicates if the request should be $batched",
            feel = FeelMode.optional)
        Boolean batch,
    @TemplateProperty(
            group = "batch",
            label = "Batch Request Payload",
            feel = FeelMode.optional,
            optional = true,
            type = TemplateProperty.PropertyType.Text,
            description = "Provide the payload for the batch request",
            condition =
                @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=true"))
        String batchRequestPayload) {

  @TemplateDiscriminatorProperty(
      group = "sap",
      label = "OData version",
      description = "what version of the OData protocol the above service uses",
      name = "oDataVersion")
  public enum ODataVersion {
    V2,
    V4
  }

  @JsonTypeInfo(use = Id.NAME, property = "httpMethod")
  @JsonSubTypes({
    @Type(value = HttpMethod.Get.class, name = "get"),
    @Type(value = HttpMethod.Post.class, name = "post"),
    @Type(value = HttpMethod.Put.class, name = "put"),
    @Type(value = HttpMethod.Patch.class, name = "patch"),
    @Type(value = HttpMethod.Delete.class, name = "delete")
  })
  @TemplateDiscriminatorProperty(
      name = "httpMethod",
      label = "HTTP method",
      description = "read, write, update or delete operation",
      group = "sap",
      defaultValue = "get")
  public sealed interface HttpMethod {

    @TemplateSubType(id = "get", label = "GET")
    record Get(
        @TemplateProperty(
                group = "advanced",
                label = "$filter",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                description = "$filter on EntitySet",
                optional = true)
            String filter,
        @TemplateProperty(
                group = "advanced",
                label = "$top",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                description = "only the first $top results of an EntitySet",
                optional = true)
            Long top,
        @TemplateProperty(
                group = "advanced",
                label = "$skip",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                description = "skip the first $skip results of an EntitySet",
                optional = true)
            Long skip,
        @TemplateProperty(
                group = "advanced",
                label = "$orderby",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                description = "order the EntitySet by one or more properties",
                optional = true)
            String orderby,
        @TemplateProperty(
                group = "advanced",
                label = "$expand",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                description = "expand an Entity/-Set by another Entity/-Set",
                optional = true)
            String expand,
        @TemplateProperty(
                group = "advanced",
                label = "$select",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                description = "only select $select properties of an Entity/-Set",
                optional = true)
            @Pattern(regexp = "^\\S*$", message = "must not contain any whitespace!")
            String select,
        @Valid ODataVersionGet oDataVersionGet)
        implements HttpMethod {

      @JsonTypeInfo(use = Id.NAME, property = "oDataVersionGet")
      @JsonSubTypes({
        @Type(value = ODataVersionGet.V2.class, name = "V2"),
        @Type(value = ODataVersionGet.V4.class, name = "V4")
      })
      @TemplateDiscriminatorProperty(
          group = "sap",
          label = "OData version",
          description = "what version of the OData protocol the above service uses",
          name = "oDataVersionGet",
          defaultValue = "V2")
      public sealed interface ODataVersionGet {

        @TemplateSubType(id = "V2", label = "V2")
        record V2(
            @TemplateProperty(
                    group = "advanced",
                    label = "$inlinecount",
                    description = "$inlinecount result in EntitySet",
                    condition =
                        @TemplateProperty.PropertyCondition(
                            property = "batchReq",
                            equals = "=false"),
                    optional = true)
                Boolean inlinecount)
            implements ODataVersionGet {}

        @TemplateSubType(id = "V4", label = "V4")
        record V4(
            @TemplateProperty(
                    group = "advanced",
                    label = "$search",
                    description = "search for $search in EntitySet",
                    condition =
                        @TemplateProperty.PropertyCondition(
                            property = "batchReq",
                            equals = "=false"),
                    optional = true)
                String search,
            @TemplateProperty(
                    group = "advanced",
                    label = "$count",
                    description = "$count of EntitySet",
                    condition =
                        @TemplateProperty.PropertyCondition(
                            property = "batchReq",
                            equals = "=false"),
                    optional = true)
                Boolean count)
            implements ODataVersionGet {}
      }
    }

    @TemplateSubType(id = "post", label = "POST")
    record Post(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                description = "what version of the OData protocol the above service uses",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                defaultValue = "V2")
            ODataVersion oDataVersionPost)
        implements HttpMethod {}

    @TemplateSubType(id = "put", label = "PUT")
    record Put(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                description = "what version of the OData protocol the above service uses",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                defaultValue = "V2")
            ODataVersion oDataVersionPut)
        implements HttpMethod {}

    @TemplateSubType(id = "patch", label = "PATCH")
    record Patch(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                description = "what version of the OData protocol the above service uses",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                defaultValue = "V2")
            ODataVersion oDataVersionPatch)
        implements HttpMethod {}

    @TemplateSubType(id = "delete", label = "DELETE")
    record Delete(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                condition =
                    @TemplateProperty.PropertyCondition(property = "batchReq", equals = "=false"),
                description = "what version of the OData protocol the above service uses",
                defaultValue = "V2")
            ODataVersion oDataVersionDelete)
        implements HttpMethod {}
  }
}
